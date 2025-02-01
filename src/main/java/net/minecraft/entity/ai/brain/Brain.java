package net.minecraft.entity.ai.brain;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.annotation.Debug;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * A brain is associated with each living entity.
 * 
 * <p>A brain has {@linkplain #memories memories}, {@linkplain #sensors sensors},
 * and {@linkplain #tasks tasks}. In general, the brain can use sensors to refresh
 * the memories over time, and the memories can be shared by different tasks,
 * which can reduce duplicate calculation. For instance, instead of having each
 * task scan for the player, the memories can hold information about nearby player,
 * and the task can choose to run or not accordingly.
 * 
 * <p>To construct a brain, you need to specify the memory (module) types and
 * sensors present in a brain, and then you can add individual tasks.
 * 
 * @see LivingEntity#brain
 */

/**

 每个生物实体都关联着一个大脑。

 <p>大脑包含 {@linkplain #memories 记忆}、{@linkplain #sensors 传感器} 和 {@linkplain #tasks 任务}。
 通常，大脑可以通过传感器随时间更新记忆，而这些记忆可以被不同的任务共享，

 从而减少重复计算。例如，与其让每个任务都扫描玩家，不如让记忆存储附近玩家的信息，

 任务可以根据这些信息决定是否运行。

 <p>要构建一个大脑，你需要指定大脑中的记忆（模块）类型和传感器，
 然后可以添加具体的任务。
 */

//泛型E,继承LivingEntity
public class Brain<E extends LivingEntity> {
	//日志,无用
	static final Logger LOGGER = LogUtils.getLogger();
	//序列化Brain<E>
	private final Supplier<Codec<Brain<E>>> codecSupplier;
	//无用,意义不明
	private static final int ACTIVITY_REFRESH_COOLDOWN = 20;
	//MAP 存储Memory,Key:MemoryModuleType<?>类型 Value:记忆内容
	private final Map<MemoryModuleType<?>, Optional<? extends Memory<?>>> memories = Maps.<MemoryModuleType<?>, Optional<? extends Memory<?>>>newHashMap();
	//MAP 存储传感器Sensor, Key:SensorType<? extends Sensor<? super E>>类型 Value:传感器实例
	private final Map<SensorType<? extends Sensor<? super E>>, Sensor<? super E>> sensors = Maps.<SensorType<? extends Sensor<? super E>>, Sensor<? super E>>newLinkedHashMap();
	/**
	 * MAP 存储任务tasks
	 * Integer:优先级
	 * Activity:所属活动
	 * Set<Task<? super E>>:动下的任务集合
	 */
	private final Map<Integer, Map<Activity, Set<Task<? super E>>>> tasks = Maps.newTreeMap();

	//活动日程安排,并初始化IDLE状态
	private Schedule schedule = Schedule.EMPTY;
	//存储每个活动所需的内存状态
	//Key:活动
	//Value:该活动所需的记忆类型及其状态
	private final Map<Activity, Set<Pair<MemoryModuleType<?>, MemoryModuleState>>> requiredActivityMemories = Maps.<Activity, Set<Pair<MemoryModuleType<?>, MemoryModuleState>>>newHashMap();
	/**
	 * The map from activities to the memories to forget after the activity is
	 * completed.
	 * 存放每个活动完成后需要遗忘的记忆类型
	 * Key:活动
	 * Value:需要遗忘的记忆类型
	 */
	private final Map<Activity, Set<MemoryModuleType<?>>> forgettingActivityMemories = Maps.<Activity, Set<MemoryModuleType<?>>>newHashMap();
	//核心活动集合
	private Set<Activity> coreActivities = Sets.<Activity>newHashSet();
	//可能的活动集合
	private final Set<Activity> possibleActivities = Sets.<Activity>newHashSet();
	//默认活动,通常是IDLE
	private Activity defaultActivity = Activity.IDLE;
	//活动的开始时间 -9999
	private long activityStartTime = -9999L;


	/**
	 * 工厂方法createProfile
	 * @param memoryModules Collection,包含记忆类型
	 * @param sensors Collection,包含传感器类型
	 * @return Brain.Profile<E>对象
	 * @param <E> E
	 */
	public static <E extends LivingEntity> Profile<E> createProfile(
		Collection<? extends MemoryModuleType<?>> memoryModules,
		Collection<? extends SensorType<? extends Sensor<? super E>>> sensors
	) {
		return new Profile<>(memoryModules, sensors);
	}

	/**
	 * 创建大脑Codec
	 * @param memoryModules Collection,包含记忆类型
	 * @param sensors Collection,包含传感器类型
	 * @return Codec<Brain<E>>,用于反序列化Brain<E>
	 * @param <E> E
	 */
	public static <E extends LivingEntity> Codec<Brain<E>> createBrainCodec(
		Collection<? extends MemoryModuleType<?>> memoryModules,
		Collection<? extends SensorType<? extends Sensor<? super E>>> sensors
	) {
		//def MutableObject 容器,存放结果
		final MutableObject<Codec<Brain<E>>> mutableObject = new MutableObject<>();

		mutableObject.setValue(
			//创建MapCodec<Brain<E>>,转换为Codec<Brain<E>>
			(new MapCodec<Brain<E>>() {
				 	/**
				  	* 返回所有记忆模块类型,MemoryModuleType<?>的Key
				  	* @param ops
				  	* @return Stream流
				  	* @param <T>
				    */
					@Override
					public <T> Stream<T> keys(DynamicOps<T> ops) {
						//使用memoryModules.stream 遍历memoryModules
						return memoryModules.stream()
							//memoryType -> memoryType.getCodec():      调用memoryType的getCodec(),获取code
							//.map(codec -> Registries.MEMORY_MODULE_TYPE.getId(memoryType)):        存在？调用 Registries.MEMORY_MODULE_TYPE.getId(memoryType)获取记忆模块类型的注册 ID
							.flatMap(memoryType -> memoryType.getCodec().map(codec -> Registries.MEMORY_MODULE_TYPE.getId(memoryType)).stream())
							//转字符串，ops.createString(id.toString())包装
							.map(id -> ops.createString(id.toString()));
					}

					/**
				 	* 将原始数据解码为 Brain<E> 对象
				 	* @param ops
				 	* @param map
				 	* @return Brain<E>
				 	* @param <T>
				 	*/
					@Override
					public <T> DataResult<Brain<E>> decode(DynamicOps<T> ops, MapLike<T> map) {
						//INIT MutableObject,存储DataResult<Builder<Brain.MemoryEntry<?>>>
						MutableObject<DataResult<Builder<MemoryEntry<?>>>> mutableObject = new MutableObject<>(DataResult.success(ImmutableList.builder()));
						//遍历map.entries()
						map.entries().forEach(pair -> {
							//解析pair.getFirst() and pair.getSecond()
							DataResult<MemoryModuleType<?>> dataResult = Registries.MEMORY_MODULE_TYPE.getCodec().parse(ops, (T)pair.getFirst());
							DataResult<? extends MemoryEntry<?>> dataResult2 = dataResult.flatMap(memoryType -> this.parse(memoryType, ops, (T)pair.getSecond()));
							//添加到mutableObject
							mutableObject.setValue(mutableObject.getValue().apply2(Builder::add, dataResult2));
						});
						ImmutableList<MemoryEntry<?>> immutableList = (ImmutableList<MemoryEntry<?>>)mutableObject.getValue()
							.resultOrPartial(Brain.LOGGER::error)
							.map(Builder::build)
							.orElseGet(ImmutableList::of);
						return DataResult.success(new Brain<>(memoryModules, sensors, immutableList, mutableObject::getValue));
					}

				/**
				 * 解析单个记忆模块的值
				 * @param memoryType
				 * @param ops
				 * @param value
				 * @return
				 * @param <T>
				 * @param <U>
				 */
					@SuppressWarnings({"rawtypes", "unchecked"})
                    private <T, U> DataResult<MemoryEntry<U>> parse(MemoryModuleType<U> memoryType, DynamicOps<T> ops, T value) {
						//获取记忆的codec
						return ((DataResult)memoryType.getCodec()
								//存在 返回success
								.map(DataResult::success)
								//不存在 返回DataResult.error(() -> "No codec for memory: " + memoryType)
								.orElseGet(() -> DataResult.error(() -> "No codec for memory: " + memoryType)))
								//codec解析数据 ，返回DataResult<U>
								.flatMap(codec -> codec.parse(ops, value))
								//构建，返回
							    .map(data -> new MemoryEntry<>(memoryType, Optional.of(data)));
					}

				/**
				 * Brain<E>编码RecordBuilder<T>
				 * @param brain
				 * @param dynamicOps
				 * @param recordBuilder
				 * @return
				 * @param <T>
				 */
					public <T> RecordBuilder<T> encode(Brain<E> brain, DynamicOps<T> dynamicOps, RecordBuilder<T> recordBuilder) {
						brain.streamMemories().forEach(entry -> entry.serialize(dynamicOps, recordBuilder));
						return recordBuilder;
					}
				})

				//将 memories 字段作为序列化和反序列化的主要字段
				.fieldOf("memories")
				.codec()
		);

		//返回结果
		return mutableObject.getValue();
	}

	/**
	 * 初始化生物大脑
	 * @param memories 记忆模块类型
	 * @param sensors 传感器类型
	 * @param memoryEntries 初始的记忆条目
	 * @param codecSupplier Brain<E>的Codec(编解码器)
	 */
	public Brain(
		Collection<? extends MemoryModuleType<?>> memories,
		Collection<? extends SensorType<? extends Sensor<? super E>>> sensors,
		ImmutableList<MemoryEntry<?>> memoryEntries,
		Supplier<Codec<Brain<E>>> codecSupplier
	) {
		this.codecSupplier = codecSupplier;

		//遍历记忆
		for (MemoryModuleType<?> memoryModuleType : memories) {
			//初始化，放进去
			this.memories.put(memoryModuleType, Optional.empty());
		}

		//遍历传感器
		for (SensorType<? extends Sensor<? super E>> sensorType : sensors) {
			//sensorType.create()创建
			//放进去
			this.sensors.put(sensorType, sensorType.create());
		}

		//处理传感器输出的记忆
		for (Sensor<? super E> sensor : this.sensors.values()) {
			//sensor.getOutputMemoryModules()获取记忆类型
			for (MemoryModuleType<?> memoryModuleType2 : sensor.getOutputMemoryModules()) {
				//添加，初始化
				this.memories.put(memoryModuleType2, Optional.empty());
			}
		}

		//应用，初始化记忆
		for (MemoryEntry<?> memoryEntry : memoryEntries) {
			//遍历后初始化
			memoryEntry.apply(this);
		}
	}

	/**
	 * 编码
	 * @param ops DynamicOps<T>->DataResult<T>
	 * @return
	 * @param <T>
	 */
	public <T> DataResult<T> encode(DynamicOps<T> ops) {
		return ((Codec)this.codecSupplier.get()).encodeStart(ops, this);
	}

	/**
	 * memories转换为Stream MemoryEntry
	 * @return Stream<MemoryEntry<?>>
	 */
	Stream<MemoryEntry<?>> streamMemories() {
		return this.memories
			.entrySet()
			.stream()
			.map(entry -> MemoryEntry.of((MemoryModuleType)entry.getKey(), (Optional<? extends Memory<?>>)entry.getValue()));
	}


	/**
	 * 检查某一个记忆是否在有值状态
	 * @param type 记忆
	 * @return
	 */
	public boolean hasMemoryModule(MemoryModuleType<?> type) {
		return this.isMemoryInState(type, MemoryModuleState.VALUE_PRESENT);
	}

	/**
	 * 所有记忆清空
	 */
	public void forgetAll() {
		//遍历所有key，清空后放回去
		this.memories.keySet().forEach(type -> this.memories.put(type, Optional.empty()));
	}

	/**
	 * 忘记一类的事情
	 * @param type
	 * @param <U>
	 */
	public <U> void forget(MemoryModuleType<U> type) {
		//清空放回去
		this.remember(type, Optional.empty());
	}

	/**
	 * 记忆，没有有效期
	 * @param type 类型
	 * @param value 内容
	 * @param <U>
	 */
	public <U> void remember(MemoryModuleType<U> type, @Nullable U value) {
		this.remember(type, Optional.ofNullable(value));
	}

	/**
	 * 带有有效期的记忆
	 * @param type 类型
	 * @param value 内容
	 * @param expiry 有效期
	 * @param <U>
	 */
	public <U> void remember(MemoryModuleType<U> type, U value, long expiry) {
		this.setMemory(type, Optional.of(Memory.timed(value, expiry)));
	}

	/**
	 * 记忆
	 * @param type 类型
	 * @param value 内容
	 * @param <U>
	 */
	public <U> void remember(MemoryModuleType<U> type, Optional<? extends U> value) {
		this.setMemory(type, value.map(Memory::permanent));
	}

	/**
	 *
	 * 设置或更新记忆
	 * @param type
	 * @param memory
	 * @param <U>
	 */
	<U> void setMemory(MemoryModuleType<U> type, Optional<? extends Memory<?>> memory) {
		//这个类型存在吗？
		if (this.memories.containsKey(type)) {
			//存在 且 为空集合
			if (memory.isPresent() && this.isEmptyCollection(((Memory)memory.get()).getValue())) {
				//移除
				this.forget(type);
			} else {
				//放进去
				this.memories.put(type, memory);
			}
		}
	}


	/**
	 * 获取已经注册的记忆的值
	 * @param type 类型
	 * @return Value
	 * @param <U> U
	 */
	public <U> Optional<U> getOptionalRegisteredMemory(MemoryModuleType<U> type) {
		//通过类型获取
		Optional<? extends Memory<?>> optional = (Optional<? extends Memory<?>>)this.memories.get(type);
		//为空吗？
		if (optional == null) {
			//空的，报错
			throw new IllegalStateException("Unregistered memory fetched: " + type);
		} else {
			//原为:return optional.map(Memory::getValue); 类型不匹配修改
			//不是空的 输出Value
			return (Optional<U>) optional.map(Memory::getValue);
		}
	}

	/**
	 * 与 getOptionalRegisteredMemory 大致相同，只是此方法对null的处理是直接返回
	 * @param type 类型
	 * @return Value
	 * @param <U>
	 */
	@Nullable //可能为null
	public <U> Optional<U> getOptionalMemory(MemoryModuleType<U> type) {
		//通过类型获取
		Optional<? extends Memory<?>> optional = (Optional<? extends Memory<?>>)this.memories.get(type);
		//是null？直接返回null
		//不是？返回Value
		return optional == null ? null : optional.map(Memory::getValue);
	}

	/**
	 * 查询记忆的有效期
	 * @param type 记忆
	 * @return 有效期
	 * @param <U> U
	 */
	public <U> long getMemoryExpiry(MemoryModuleType<U> type) {
		//根据类型获取
		Optional<? extends Memory<?>> optional = (Optional<? extends Memory<?>>)this.memories.get(type);
		//获取
		//orElse(0L); 获取不到就返回0
		return (Long)optional.map(Memory::getExpiry).orElse(0L);
	}

	/**
	 * getMemories
	 * @return Memories
	 */
	@Deprecated
	@Debug
	public Map<MemoryModuleType<?>, Optional<? extends Memory<?>>> getMemories() {
		return this.memories;
	}

	/**
	 * 检查记忆是否有值，且等于 U value
	 * @param type 类型
	 * @param value 信息
	 * @return 有值吗？
	 * @param <U>
	 */
	public <U> boolean hasMemoryModuleWithValue(MemoryModuleType<U> type, U value) {
		//不存在 直接返回false
		return !this.hasMemoryModule(type) ? false :
				//获取Value
				this.getOptionalRegisteredMemory(type).
						//过滤 =value保留 不等于就舍弃
						filter(memoryValue -> memoryValue.equals(value)).
						//检查过滤后的Optional<U>是否包含值 包含=true 不包含=false
						isPresent();
	}

	/**
	 * 这个记忆处在特定的状态吗？
	 * @param type 记忆模块
	 * @param state 状态
	 * @return
	 */
	public boolean isMemoryInState(MemoryModuleType<?> type, MemoryModuleState state) {
		Optional<? extends Memory<?>> optional = (Optional<? extends Memory<?>>)this.memories.get(type);
		return optional == null
			? false
			: state == MemoryModuleState.REGISTERED
				|| state == MemoryModuleState.VALUE_PRESENT && optional.isPresent()
				|| state == MemoryModuleState.VALUE_ABSENT && !optional.isPresent();
	}

	/**
	 * 获取日程
	 * @return 日程
	 */
	public Schedule getSchedule() {
		return this.schedule;
	}

	/**
	 * 设置日程
	 * @param schedule 日程
	 */
	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	/**
	 * 设置核心活动
	 * @param coreActivities 核心活动集合
	 */
	public void setCoreActivities(Set<Activity> coreActivities) {
		this.coreActivities = coreActivities;
	}

	/**
	 * 获取可能的活动
	 * @return 可能的活动
	 */
	@Deprecated
	@Debug
	public Set<Activity> getPossibleActivities() {
		return this.possibleActivities;
	}

	/**
	 * 获取正在执行的任务
	 * @return 任务列表
	 */
	@Deprecated
	@Debug
	public List<Task<? super E>> getRunningTasks() {
		//创建列表
		List<Task<? super E>> list = new ObjectArrayList<>();

		for (Map<Activity, Set<Task<? super E>>> map : this.tasks.values()) {
			for (Set<Task<? super E>> set : map.values()) {
				for (Task<? super E> task : set) {
					if (task.getStatus() == MultiTickTask.Status.RUNNING) {
						list.add(task);
					}
				}
			}
		}

		return list;
	}

	/**
	 * resetPossibleActivities,除了IDLE
	 */
	public void resetPossibleActivities() {
		//直接把可能进行的活动
		this.resetPossibleActivities(this.defaultActivity);
	}

	/**
	 * 获取第一个可能的任务，但是不包含核心任务
	 * @return
	 */
	public Optional<Activity> getFirstPossibleNonCoreActivity() {
		//遍历
		for (Activity activity : this.possibleActivities) {
			//检查时候在核心活动中
			if (!this.coreActivities.contains(activity)) {
				//不在，返回
				return Optional.of(activity);
			}
		}

		//没找到，返回空的Optional
		return Optional.empty();
	}

	/**
	 * 专注执行某一个activity
	 * @param activity 活动
	 */
	public void doExclusively(Activity activity) {
		//检查活动是否可以执行
		if (this.canDoActivity(activity)) {
			//reset，除了activity
			this.resetPossibleActivities(activity);
		} else {
			//直接reset，除了IDLE
			this.resetPossibleActivities();
		}
	}

	/**
	 * resetPossibleActivities
	 * @param except
	 */
	private void resetPossibleActivities(Activity except) {
		//如果包含了except这个活动，就不进行任何操作
		if (!this.hasActivity(except)) {
			//不包含

			//移除除了except的其他无用的(需要忘记的)(可擦除的)记忆
			this.forgetIrrelevantMemories(except);
			//清除列表
			this.possibleActivities.clear();
			//把核心任务添加至列表
			this.possibleActivities.addAll(this.coreActivities);
			//把原来排除的也加进去
			this.possibleActivities.add(except);
		}
	}

	/**
	 * 擦除无关的记忆
	 * @param except
	 */
	private void forgetIrrelevantMemories(Activity except) {
		//遍历
		for (Activity activity : this.possibleActivities) {
			//如果不等于except
			if (activity != except) {
				//看看活动是否处于要擦除的记忆的列表里
				Set<MemoryModuleType<?>> set = (Set<MemoryModuleType<?>>)this.forgettingActivityMemories.get(activity);
				//找到了
				if (set != null) {
					//遍历set中的记忆类型
					for (MemoryModuleType<?> memoryModuleType : set) {
						//调用删除
						this.forget(memoryModuleType);
					}
				}
			}
		}
	}

	/**
	 * 刷新活动
	 * @param timeOfDay
	 * @param time 根据用法，传入的大部分是游戏当前时间
	 */
	//TODO 意义可能不正确或不明确，建议以后修正
	public void refreshActivities(long timeOfDay, long time) {
		//time + 9999 >20 即 距离上次切换活动已经超过 20 个tick
		if (time - this.activityStartTime > 20L) {
			//赋值
			this.activityStartTime = time;

			//timeOfDay % 24000L 计算时间 ，在日程里查找
			Activity activity = this.getSchedule().getActivityForTime((int)(timeOfDay % 24000L));
			//possibleActivities里不包含
			if (!this.possibleActivities.contains(activity)) {
				//专注执行这个
				this.doExclusively(activity);
			}
		}
	}

	/**
	 * resetPossibleActivities，逻辑相同，次方法为重写
	 * @param activities 活动集合
	 */
	public void resetPossibleActivities(List<Activity> activities) {
		for (Activity activity : activities) {
			if (this.canDoActivity(activity)) {
				this.resetPossibleActivities(activity);
				break;
			}
		}
	}

	/**
	 * 设置默认的活动
	 * @param activity 活动
	 */
	public void setDefaultActivity(Activity activity) {
		this.defaultActivity = activity;
	}


	/**
	 * setTaskList
	 * @param activity 活动
	 * @param begin 起始索引
	 * @param list task list
	 */
	public void setTaskList(Activity activity, int begin, ImmutableList<? extends Task<? super E>> list) {
		//indexTaskList给任务列表打索引
		this.setTaskList(activity, this.indexTaskList(begin, list));
	}

	/**
	 * setTaskList
	 * @param activity 活动
	 * @param begin 起始索引
	 * @param tasks task list
	 * @param memoryType 依赖的记忆
	 */
	public void setTaskList(Activity activity, int begin, ImmutableList<? extends Task<? super E>> tasks, MemoryModuleType<?> memoryType) {
		//这个memoryType要存在值
		Set<Pair<MemoryModuleType<?>, MemoryModuleState>> set = ImmutableSet.of(Pair.of(memoryType, MemoryModuleState.VALUE_PRESENT));
		//创建一个包含memoryType的不可变集合
		Set<MemoryModuleType<?>> set2 = ImmutableSet.of(memoryType);

		this.setTaskList(activity, this.indexTaskList(begin, tasks), set, set2);
	}

	/**
	 * 接收已编号任务列表
	 * @param activity 活动
	 * @param indexedTasks 编号索引的tasks
	 */
	public void setTaskList(Activity activity, ImmutableList<? extends Pair<Integer, ? extends Task<? super E>>> indexedTasks) {
		this.setTaskList(activity, indexedTasks, ImmutableSet.of(), Sets.<MemoryModuleType<?>>newHashSet());
	}

	/**
	 * setTaskLis
	 * @param activity 活动
	 * @param indexedTasks tasks
	 * @param requiredMemories 执行前所需要的记忆和状态
	 */
	public void setTaskList(
		Activity activity,
		ImmutableList<? extends Pair<Integer, ? extends Task<? super E>>> indexedTasks,
		Set<Pair<MemoryModuleType<?>, MemoryModuleState>> requiredMemories
	) {
		this.setTaskList(activity, indexedTasks, requiredMemories, Sets.<MemoryModuleType<?>>newHashSet());
	}

	/**
	 * 最终的setTaskLis
	 * @param activity 活动
	 * @param indexedTasks 编好号的tasks
	 * @param requiredMemories 执行前所需要的记忆和状态
	 * @param forgettingMemories 任务结束后要忘记的记忆
	 */
	@SuppressWarnings("unchecked")
    public void setTaskList(
		Activity activity,
		ImmutableList<? extends Pair<Integer, ? extends Task<? super E>>> indexedTasks,
		Set<Pair<MemoryModuleType<?>, MemoryModuleState>> requiredMemories,
		Set<MemoryModuleType<?>> forgettingMemories
	) {
		//把对应任务的条件添加到requiredActivityMemories
		this.requiredActivityMemories.put(activity, requiredMemories);
		//如果不为空
		if (!forgettingMemories.isEmpty()) {
			//存进去
			this.forgettingActivityMemories.put(activity, forgettingMemories);
		}


		for (Pair<Integer, ? extends Task<? super E>> pair : indexedTasks) {
			//pair.getFirst() 按照任务编号分裂
			((Set)((Map)this.tasks.computeIfAbsent(pair.getFirst(), index -> Maps.newHashMap())).
					//按照活动分类
					computeIfAbsent(activity, activity2
							//使用newLinkedHashSet按顺序存入任务
							-> Sets.newLinkedHashSet()))
				.add(pair.getSecond());
		}
	}

	/**
	 * 清空任务
	 */
	@VisibleForTesting
	public void clear() {
		this.tasks.clear();
	}

	/**
	 * 检查可能活动列表里是否包含指定的
	 * @param activity 活动
	 * @return 是否包含
	 */
	public boolean hasActivity(Activity activity) {
		//检查
		return this.possibleActivities.contains(activity);
	}

	/**
	 * 创建copy 只copy记忆和传感器 任务不copy
	 * @return result
	 */
	public Brain<E> copy() {
		Brain<E> brain = new Brain<>(this.memories.keySet(), this.sensors.keySet(), ImmutableList.of(), this.codecSupplier);

		for (Entry<MemoryModuleType<?>, Optional<? extends Memory<?>>> entry : this.memories.entrySet()) {
			MemoryModuleType<?> memoryModuleType = (MemoryModuleType<?>)entry.getKey();
			if (((Optional)entry.getValue()).isPresent()) {
				brain.memories.put(memoryModuleType, (Optional)entry.getValue());
			}
		}

		return brain;
	}

	/**
	 * tick任务
	 * @param world world
	 * @param entity entity
	 */
	public void tick(ServerWorld world, E entity) {
		//更新记忆状态
		this.tickMemories();
		//更新传感器
		this.tickSensors(world, entity);
		//开始任务
		this.startTasks(world, entity);
		//更新任务
		this.updateTasks(world, entity);
	}

	/**
	 * 更新传感器
	 * @param world ServerWorld
	 * @param entity entity
	 */
	private void tickSensors(ServerWorld world, E entity) {
		//遍历
		for (Sensor<? super E> sensor : this.sensors.values()) {
			sensor.tick(world, entity);
		}
	}

	/**
	 * 更新记忆的状态
	 */
	private void tickMemories() {
		//遍历所有记忆的kv
		for (Entry<MemoryModuleType<?>, Optional<? extends Memory<?>>> entry : this.memories.entrySet()) {
			//检查value是否为空
			if (((Optional)entry.getValue()).isPresent()) {
				//存在值

				//获取记忆的value Memory<?>
				Memory<?> memory = (Memory<?>)((Optional)entry.getValue()).get();
				//过期了？
				if (memory.isExpired()) {
					//忘记他
					this.forget((MemoryModuleType)entry.getKey());
				}

				//没过期，更新即可
				memory.tick();
			}
		}
	}

	/**
	 * 停止所有task
	 * @param world ServerWorld
	 * @param entity 实体
	 */
	public void stopAllTasks(ServerWorld world, E entity) {
		//获取时间
		long l = entity.getWorld().getTime();

		//获取所有正在运行的任务
		for (Task<? super E> task : this.getRunningTasks()) {
			//stop
			task.stop(world, entity, l);
		}
	}

	/**
	 * 开始任务
	 * @param world ServerWorld
	 * @param entity entity
	 */
	//TODO 详细解释
	private void startTasks(ServerWorld world, E entity) {
		long l = world.getTime();

		for (Map<Activity, Set<Task<? super E>>> map : this.tasks.values()) {
			for (Entry<Activity, Set<Task<? super E>>> entry : map.entrySet()) {
				Activity activity = (Activity)entry.getKey();
				if (this.possibleActivities.contains(activity)) {
					for (Task<? super E> task : (Set)entry.getValue()) {
						if (task.getStatus() == MultiTickTask.Status.STOPPED) {
							task.tryStarting(world, entity, l);
						}
					}
				}
			}
		}
	}

	/**
	 * 更新任务
	 * @param world ServerWorld
	 * @param entity entity
	 */
	//TODO 详细解释
	private void updateTasks(ServerWorld world, E entity) {
		long l = world.getTime();

		for (Task<? super E> task : this.getRunningTasks()) {
			task.tick(world, entity, l);
		}
	}


	/**
	 * 检查是否可以执行活动
	 * @param activity 活动
	 * @return 结果
	 */
	private boolean canDoActivity(Activity activity) {
		//检查是否存在于requiredActivityMemories，不存在直接返回false
		if (!this.requiredActivityMemories.containsKey(activity)) {
			return false;
		} else {
			//遍历出活动需要的MemoryModuleType<?>:需要什么记忆 MemoryModuleState:这个记忆的状态
			for (Pair<MemoryModuleType<?>, MemoryModuleState> pair : (Set)this.requiredActivityMemories.get(activity)) {
				MemoryModuleType<?> memoryModuleType = pair.getFirst();
				MemoryModuleState memoryModuleState = pair.getSecond();
				if (!this.isMemoryInState(memoryModuleType, memoryModuleState)) {
					//不在应该有的状态，返回false
					return false;
				}
			}

			return true;
		}
	}

	/**
	 * 检查是否是空集合
	 * @param value
	 * @return
	 */
	private boolean isEmptyCollection(Object value) {
		return value instanceof Collection && ((Collection)value).isEmpty();
	}

	/**
	 * 打索引
	 * @param begin 任务索引的开头，独占
	 * @param begin the beginning of the index of tasks, exclusive
	 */
	//TODO 详细解释
	ImmutableList<? extends Pair<Integer, ? extends Task<? super E>>> indexTaskList(int begin, ImmutableList<? extends Task<? super E>> tasks) {
		int i = begin;
		Builder<Pair<Integer, ? extends Task<? super E>>> builder = ImmutableList.builder();

		for (Task<? super E> task : tasks) {
			builder.add(Pair.of(i++, task));
		}

		return builder.build();
	}

	//内部辅助类
	static final class MemoryEntry<U> {
		//记忆类型
		private final MemoryModuleType<U> type;
		//记忆数据
		private final Optional<? extends Memory<U>> data;

		/**
		 * 创建MemoryEntry
		 * @param type 类型
		 * @param data 数据
		 * @return result
		 * @param <U>
		 */
		static <U> MemoryEntry<U> of(MemoryModuleType<U> type, Optional<? extends Memory<?>> data) {
			return new MemoryEntry<>(type, (Optional<? extends Memory<U>>)data);
		}

		/**
		 * 构造函数
		 * @param type
		 * @param data
		 */
		MemoryEntry(MemoryModuleType<U> type, Optional<? extends Memory<U>> data) {
			this.type = type;
			this.data = data;
		}

		/**
		 * 设置，更新记忆
		 * @param brain 大脑
		 */
		void apply(Brain<?> brain) {
			brain.setMemory(this.type, this.data);
		}

		//持久化
		public <T> void serialize(DynamicOps<T> ops, RecordBuilder<T> builder) {
			this.type
				.getCodec()
				.ifPresent(
					codec -> this.data.ifPresent(data -> builder.add(Registries.MEMORY_MODULE_TYPE.getCodec().encodeStart(ops, this.type), codec.encodeStart(ops, data)))
				);
		}
	}

	/**
	 * A simple profile of a brain. Indicates what types of memory modules and
	 * sensors a brain can have.
	 * 大脑的简单轮廓。指示内存模块的类型，以及大脑可以拥有的传感器。
	 */
	public static final class Profile<E extends LivingEntity> {
		private final Collection<? extends MemoryModuleType<?>> memoryModules;
		private final Collection<? extends SensorType<? extends Sensor<? super E>>> sensors;
		private final Codec<Brain<E>> codec;

		Profile(Collection<? extends MemoryModuleType<?>> memoryModules, Collection<? extends SensorType<? extends Sensor<? super E>>> sensors) {
			this.memoryModules = memoryModules;
			this.sensors = sensors;
			this.codec = Brain.createBrainCodec(memoryModules, sensors);
		}

		public Brain<E> deserialize(Dynamic<?> data) {
			return (Brain<E>)this.codec
				.parse(data)
				.resultOrPartial(Brain.LOGGER::error)
				.orElseGet(() -> new Brain(this.memoryModules, this.sensors, ImmutableList.of(), () -> this.codec));
		}
	}
}
