package net.minecraft.world.biome.source.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import org.jetbrains.annotations.Nullable;

public class MultiNoiseUtil {
	private static final boolean field_34477 = false;
	private static final float TO_LONG_FACTOR = 10000.0F;
	@VisibleForTesting
	protected static final int HYPERCUBE_DIMENSION = 7;

	public static NoiseValuePoint createNoiseValuePoint(
		float temperatureNoise, float humidityNoise, float continentalnessNoise, float erosionNoise, float depth, float weirdnessNoise
	) {
		return new NoiseValuePoint(
			toLong(temperatureNoise), toLong(humidityNoise), toLong(continentalnessNoise), toLong(erosionNoise), toLong(depth), toLong(weirdnessNoise)
		);
	}

	public static NoiseHypercube createNoiseHypercube(
		float temperature, float humidity, float continentalness, float erosion, float depth, float weirdness, float offset
	) {
		return new NoiseHypercube(
			ParameterRange.of(temperature),
			ParameterRange.of(humidity),
			ParameterRange.of(continentalness),
			ParameterRange.of(erosion),
			ParameterRange.of(depth),
			ParameterRange.of(weirdness),
			toLong(offset)
		);
	}

	public static NoiseHypercube createNoiseHypercube(
		ParameterRange temperature,
		ParameterRange humidity,
		ParameterRange continentalness,
		ParameterRange erosion,
		ParameterRange depth,
		ParameterRange weirdness,
		float offset
	) {
		return new NoiseHypercube(temperature, humidity, continentalness, erosion, depth, weirdness, toLong(offset));
	}

	public static long toLong(float value) {
		return (long)(value * 10000.0F);
	}

	public static float toFloat(long value) {
		return (float)value / 10000.0F;
	}

	public static MultiNoiseSampler createEmptyMultiNoiseSampler() {
		DensityFunction densityFunction = DensityFunctionTypes.zero();
		return new MultiNoiseSampler(densityFunction, densityFunction, densityFunction, densityFunction, densityFunction, densityFunction, List.of());
	}

	public static BlockPos findFittestPosition(List<NoiseHypercube> noises, MultiNoiseSampler sampler) {
		return (new FittestPositionFinder(noises, sampler)).bestResult.location();
	}

	public static class Entries<T> {
		private final List<Pair<NoiseHypercube, T>> entries;
		private final SearchTree<T> tree;

		public static <T> Codec<Entries<T>> createCodec(MapCodec<T> entryCodec) {
			return Codecs.nonEmptyList(
					RecordCodecBuilder.<T>create(
							instance -> instance.group(NoiseHypercube.CODEC.fieldOf("parameters").forGetter(Pair::getFirst), entryCodec.forGetter(Pair::getSecond))
									.apply(instance, Pair::of)
						)
						.listOf()
				)
				.xmap(Entries::new, Entries::getEntries);
		}

		public Entries(List<Pair<NoiseHypercube, T>> entries) {
			this.entries = entries;
			this.tree = SearchTree.create(entries);
		}

		public List<Pair<NoiseHypercube, T>> getEntries() {
			return this.entries;
		}

		/**
		 * {@return the closest entry at the given point}.
		 * 
		 * @param point the point of all relevant noises
		 */
		public T get(NoiseValuePoint point) {
			return this.getValue(point);
		}

		/**
		 * {@return the closest entry at the given point}.
		 * 
		 * Note that this method only exists for testing, and is usually a lot slower
		 * than {@link #getValue}.
		 */
		@VisibleForTesting
		public T getValueSimple(NoiseValuePoint point) {
			Iterator<Pair<NoiseHypercube, T>> iterator = this.getEntries().iterator();
			Pair<NoiseHypercube, T> pair = (Pair<NoiseHypercube, T>)iterator.next();
			long l = pair.getFirst().getSquaredDistance(point);
			T object = pair.getSecond();

			while (iterator.hasNext()) {
				Pair<NoiseHypercube, T> pair2 = (Pair<NoiseHypercube, T>)iterator.next();
				long m = pair2.getFirst().getSquaredDistance(point);
				if (m < l) {
					l = m;
					object = pair2.getSecond();
				}
			}

			return object;
		}

		/**
		 * {@return the closest entry at the given point}.
		 * 
		 * @param point the point of all relevant noises
		 */
		public T getValue(NoiseValuePoint point) {
			return this.getValue(point, SearchTree.TreeNode::getSquaredDistance);
		}

		protected T getValue(NoiseValuePoint point, NodeDistanceFunction<T> distanceFunction) {
			return this.tree.get(point, distanceFunction);
		}
	}

	static class FittestPositionFinder {
		Result bestResult;

		FittestPositionFinder(List<NoiseHypercube> noises, MultiNoiseSampler sampler) {
			this.bestResult = calculateFitness(noises, sampler, 0, 0);
			this.findFittest(noises, sampler, 2048.0F, 512.0F);
			this.findFittest(noises, sampler, 512.0F, 32.0F);
		}

		private void findFittest(List<NoiseHypercube> noises, MultiNoiseSampler sampler, float maxDistance, float step) {
			float f = 0.0F;
			float g = step;
			BlockPos blockPos = this.bestResult.location();

			while (g <= maxDistance) {
				int i = blockPos.getX() + (int)(Math.sin((double)f) * (double)g);
				int j = blockPos.getZ() + (int)(Math.cos((double)f) * (double)g);
				Result result = calculateFitness(noises, sampler, i, j);
				if (result.fitness() < this.bestResult.fitness()) {
					this.bestResult = result;
				}

				f += step / g;
				if ((double)f > Math.PI * 2) {
					f = 0.0F;
					g += step;
				}
			}
		}

		private static Result calculateFitness(
                List<NoiseHypercube> noises, MultiNoiseSampler sampler, int x, int z
		) {
			double d = MathHelper.square(2500.0);
			int i = 2;
			long l = (long)((double)MathHelper.square(10000.0F) * Math.pow((double)(MathHelper.square((long)x) + MathHelper.square((long)z)) / d, 2.0));
			NoiseValuePoint noiseValuePoint = sampler.sample(BiomeCoords.fromBlock(x), 0, BiomeCoords.fromBlock(z));
			NoiseValuePoint noiseValuePoint2 = new NoiseValuePoint(
				noiseValuePoint.temperatureNoise(),
				noiseValuePoint.humidityNoise(),
				noiseValuePoint.continentalnessNoise(),
				noiseValuePoint.erosionNoise(),
				0L,
				noiseValuePoint.weirdnessNoise()
			);
			long m = Long.MAX_VALUE;

			for (NoiseHypercube noiseHypercube : noises) {
				m = Math.min(m, noiseHypercube.getSquaredDistance(noiseValuePoint2));
			}

			return new Result(new BlockPos(x, 0, z), l + m);
		}

		static record Result(BlockPos location, long fitness) {
		}
	}

	public static record MultiNoiseSampler(
		DensityFunction temperature,
		DensityFunction humidity,
		DensityFunction continentalness,
		DensityFunction erosion,
		DensityFunction depth,
		DensityFunction weirdness,
		List<NoiseHypercube> spawnTarget
	) {
		public NoiseValuePoint sample(int x, int y, int z) {
			int i = BiomeCoords.toBlock(x);
			int j = BiomeCoords.toBlock(y);
			int k = BiomeCoords.toBlock(z);
			DensityFunction.UnblendedNoisePos unblendedNoisePos = new DensityFunction.UnblendedNoisePos(i, j, k);
			return MultiNoiseUtil.createNoiseValuePoint(
				(float)this.temperature.sample(unblendedNoisePos),
				(float)this.humidity.sample(unblendedNoisePos),
				(float)this.continentalness.sample(unblendedNoisePos),
				(float)this.erosion.sample(unblendedNoisePos),
				(float)this.depth.sample(unblendedNoisePos),
				(float)this.weirdness.sample(unblendedNoisePos)
			);
		}

		public BlockPos findBestSpawnPosition() {
			return this.spawnTarget.isEmpty() ? BlockPos.ORIGIN : MultiNoiseUtil.findFittestPosition(this.spawnTarget, this);
		}
	}

	interface NodeDistanceFunction<T> {
		long getDistance(SearchTree.TreeNode<T> node, long[] otherParameters);
	}

	/**
	 * Represents a hypercube in a multi-dimensional cartesian plane. The multi-noise
	 * biome source picks the closest noise hypercube from its selected point
	 * and chooses the biome associated to it.
	 */
	public static record NoiseHypercube(
		ParameterRange temperature,
		ParameterRange humidity,
		ParameterRange continentalness,
		ParameterRange erosion,
		ParameterRange depth,
		ParameterRange weirdness,
		long offset
	) {
		public static final Codec<NoiseHypercube> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						ParameterRange.CODEC.fieldOf("temperature").forGetter(noiseHypercube -> noiseHypercube.temperature),
						ParameterRange.CODEC.fieldOf("humidity").forGetter(noiseHypercube -> noiseHypercube.humidity),
						ParameterRange.CODEC.fieldOf("continentalness").forGetter(noiseHypercube -> noiseHypercube.continentalness),
						ParameterRange.CODEC.fieldOf("erosion").forGetter(noiseHypercube -> noiseHypercube.erosion),
						ParameterRange.CODEC.fieldOf("depth").forGetter(noiseHypercube -> noiseHypercube.depth),
						ParameterRange.CODEC.fieldOf("weirdness").forGetter(noiseHypercube -> noiseHypercube.weirdness),
						Codec.floatRange(0.0F, 1.0F).fieldOf("offset").xmap(MultiNoiseUtil::toLong, MultiNoiseUtil::toFloat).forGetter(noiseHypercube -> noiseHypercube.offset)
					)
					.apply(instance, NoiseHypercube::new)
		);

		/**
		 * Calculates the distance from this noise point to another one. The
		 * distance is a squared distance in a multi-dimensional cartesian plane
		 * from a mathematical point of view, with a special parameter that
		 * reduces the calculated distance.
		 * 
		 * <p>For most fields except weight, smaller difference between
		 * two points' fields will lead to smaller distance. For weight,
		 * larger differences lead to smaller distance.
		 * 
		 * <p>This distance is used by the mixed-noise biome layer source. The
		 * layer source calculates an arbitrary noise point, and selects the
		 * biome that offers a closest point to its arbitrary point.
		 */
		long getSquaredDistance(NoiseValuePoint point) {
			return MathHelper.square(this.temperature.getDistance(point.temperatureNoise))
				+ MathHelper.square(this.humidity.getDistance(point.humidityNoise))
				+ MathHelper.square(this.continentalness.getDistance(point.continentalnessNoise))
				+ MathHelper.square(this.erosion.getDistance(point.erosionNoise))
				+ MathHelper.square(this.depth.getDistance(point.depth))
				+ MathHelper.square(this.weirdness.getDistance(point.weirdnessNoise))
				+ MathHelper.square(this.offset);
		}

		protected List<ParameterRange> getParameters() {
			return ImmutableList.of(
				this.temperature,
				this.humidity,
				this.continentalness,
				this.erosion,
				this.depth,
				this.weirdness,
				new ParameterRange(this.offset, this.offset)
			);
		}
	}

	public static record NoiseValuePoint(long temperatureNoise, long humidityNoise, long continentalnessNoise, long erosionNoise, long depth, long weirdnessNoise) {

		@VisibleForTesting
		protected long[] getNoiseValueList() {
			return new long[]{this.temperatureNoise, this.humidityNoise, this.continentalnessNoise, this.erosionNoise, this.depth, this.weirdnessNoise, 0L};
		}
	}

	public static record ParameterRange(long min, long max) {
		public static final Codec<ParameterRange> CODEC = Codecs.createCodecForPairObject(
			Codec.floatRange(-2.0F, 2.0F),
			"min",
			"max",
			(min, max) -> min.compareTo(max) > 0
					? DataResult.error(() -> "Cannon construct interval, min > max (" + min + " > " + max + ")")
					: DataResult.success(new ParameterRange(MultiNoiseUtil.toLong(min), MultiNoiseUtil.toLong(max))),
			parameterRange -> MultiNoiseUtil.toFloat(parameterRange.min()),
			parameterRange -> MultiNoiseUtil.toFloat(parameterRange.max())
		);

		public static ParameterRange of(float point) {
			return of(point, point);
		}

		public static ParameterRange of(float min, float max) {
			if (min > max) {
				throw new IllegalArgumentException("min > max: " + min + " " + max);
			} else {
				return new ParameterRange(MultiNoiseUtil.toLong(min), MultiNoiseUtil.toLong(max));
			}
		}

		/**
		 * Creates a new {@link ParameterRange} that combines the parameters.
		 * 
		 * @return the created parameter range.
		 * 
		 * @param min this will be used for the created range's minimum value
		 * @param max this will be used for the created range's maximum value
		 */
		public static ParameterRange combine(ParameterRange min, ParameterRange max) {
			if (min.min() > max.max()) {
				throw new IllegalArgumentException("min > max: " + min + " " + max);
			} else {
				return new ParameterRange(min.min(), max.max());
			}
		}

		public String toString() {
			return this.min == this.max ? String.format(Locale.ROOT, "%d", this.min) : String.format(Locale.ROOT, "[%d-%d]", this.min, this.max);
		}

		public long getDistance(long noise) {
			long l = noise - this.max;
			long m = this.min - noise;
			return l > 0L ? l : Math.max(m, 0L);
		}

		public long getDistance(ParameterRange other) {
			long l = other.min() - this.max;
			long m = this.min - other.max();
			return l > 0L ? l : Math.max(m, 0L);
		}

		public ParameterRange combine(@Nullable MultiNoiseUtil.ParameterRange other) {
			return other == null ? this : new ParameterRange(Math.min(this.min, other.min()), Math.max(this.max, other.max()));
		}
	}

	protected static final class SearchTree<T> {
		private static final int MAX_NODES_FOR_SIMPLE_TREE = 6;
		private final TreeNode<T> firstNode;
		private final ThreadLocal<TreeLeafNode<T>> previousResultNode = new ThreadLocal();

		private SearchTree(TreeNode<T> firstNode) {
			this.firstNode = firstNode;
		}

		public static <T> SearchTree<T> create(List<Pair<NoiseHypercube, T>> entries) {
			if (entries.isEmpty()) {
				throw new IllegalArgumentException("Need at least one value to build the search tree.");
			} else {
				int i = ((NoiseHypercube)((Pair)entries.get(0)).getFirst()).getParameters().size();
				if (i != 7) {
					throw new IllegalStateException("Expecting parameter space to be 7, got " + i);
				} else {
					List<TreeLeafNode<T>> list = (List<TreeLeafNode<T>>)entries.stream()
						.map(entry -> new TreeLeafNode<>((NoiseHypercube)entry.getFirst(), entry.getSecond()))
						.collect(Collectors.toCollection(ArrayList::new));
					return new SearchTree<>(createNode(i, list));
				}
			}
		}

		private static <T> TreeNode<T> createNode(int parameterNumber, List<? extends TreeNode<T>> subTree) {
			if (subTree.isEmpty()) {
				throw new IllegalStateException("Need at least one child to build a node");
			} else if (subTree.size() == 1) {
				return (TreeNode<T>)subTree.get(0);
			} else if (subTree.size() <= 6) {
				subTree.sort(Comparator.comparingLong(node -> {
					long lx = 0L;

					for (int jx = 0; jx < parameterNumber; jx++) {
						ParameterRange parameterRange = node.parameters[jx];
						lx += Math.abs((parameterRange.min() + parameterRange.max()) / 2L);
					}

					return lx;
				}));
				return new TreeBranchNode<>(subTree);
			} else {
				long l = Long.MAX_VALUE;
				int i = -1;
				List<TreeBranchNode<T>> list = null;

				for (int j = 0; j < parameterNumber; j++) {
					sortTree(subTree, parameterNumber, j, false);
					List<TreeBranchNode<T>> list2 = getBatchedTree(subTree);
					long m = 0L;

					for (TreeBranchNode<T> treeBranchNode : list2) {
						m += getRangeLengthSum(treeBranchNode.parameters);
					}

					if (l > m) {
						l = m;
						i = j;
						list = list2;
					}
				}

				sortTree(list, parameterNumber, i, true);
				return new TreeBranchNode<>(
					(List<? extends TreeNode<T>>)list.stream()
						.map(node -> createNode(parameterNumber, Arrays.asList(node.subTree)))
						.collect(Collectors.toList())
				);
			}
		}

		private static <T> void sortTree(List<? extends TreeNode<T>> subTree, int parameterNumber, int currentParameter, boolean abs) {
			Comparator<TreeNode<T>> comparator = createNodeComparator(currentParameter, abs);

			for (int i = 1; i < parameterNumber; i++) {
				comparator = comparator.thenComparing(createNodeComparator((currentParameter + i) % parameterNumber, abs));
			}

			subTree.sort(comparator);
		}

		private static <T> Comparator<TreeNode<T>> createNodeComparator(int currentParameter, boolean abs) {
			return Comparator.comparingLong(treeNode -> {
				ParameterRange parameterRange = treeNode.parameters[currentParameter];
				long l = (parameterRange.min() + parameterRange.max()) / 2L;
				return abs ? Math.abs(l) : l;
			});
		}

		private static <T> List<TreeBranchNode<T>> getBatchedTree(List<? extends TreeNode<T>> nodes) {
			List<TreeBranchNode<T>> list = Lists.<TreeBranchNode<T>>newArrayList();
			List<TreeNode<T>> list2 = Lists.<TreeNode<T>>newArrayList();
			int i = (int)Math.pow(6.0, Math.floor(Math.log((double)nodes.size() - 0.01) / Math.log(6.0)));

			for (TreeNode<T> treeNode : nodes) {
				list2.add(treeNode);
				if (list2.size() >= i) {
					list.add(new TreeBranchNode(list2));
					list2 = Lists.<TreeNode<T>>newArrayList();
				}
			}

			if (!list2.isEmpty()) {
				list.add(new TreeBranchNode(list2));
			}

			return list;
		}

		private static long getRangeLengthSum(ParameterRange[] parameters) {
			long l = 0L;

			for (ParameterRange parameterRange : parameters) {
				l += Math.abs(parameterRange.max() - parameterRange.min());
			}

			return l;
		}

		static <T> List<ParameterRange> getEnclosingParameters(List<? extends TreeNode<T>> subTree) {
			if (subTree.isEmpty()) {
				throw new IllegalArgumentException("SubTree needs at least one child");
			} else {
				int i = 7;
				List<ParameterRange> list = Lists.<ParameterRange>newArrayList();

				for (int j = 0; j < 7; j++) {
					list.add(null);
				}

				for (TreeNode<T> treeNode : subTree) {
					for (int k = 0; k < 7; k++) {
						list.set(k, treeNode.parameters[k].combine((ParameterRange)list.get(k)));
					}
				}

				return list;
			}
		}

		public T get(NoiseValuePoint point, NodeDistanceFunction<T> distanceFunction) {
			long[] ls = point.getNoiseValueList();
			TreeLeafNode<T> treeLeafNode = this.firstNode
				.getResultingNode(ls, (TreeLeafNode<T>)this.previousResultNode.get(), distanceFunction);
			this.previousResultNode.set(treeLeafNode);
			return treeLeafNode.value;
		}

		static final class TreeBranchNode<T> extends TreeNode<T> {
			final TreeNode<T>[] subTree;

			protected TreeBranchNode(List<? extends TreeNode<T>> list) {
				this(SearchTree.getEnclosingParameters(list), list);
			}

			protected TreeBranchNode(List<ParameterRange> parameters, List<? extends TreeNode<T>> subTree) {
				super(parameters);
				this.subTree = (TreeNode<T>[])subTree.toArray(new TreeNode[0]);
			}

			@Override
			protected TreeLeafNode<T> getResultingNode(
				long[] otherParameters, @Nullable MultiNoiseUtil.SearchTree.TreeLeafNode<T> alternative, NodeDistanceFunction<T> distanceFunction
			) {
				long l = alternative == null ? Long.MAX_VALUE : distanceFunction.getDistance(alternative, otherParameters);
				TreeLeafNode<T> treeLeafNode = alternative;

				for (TreeNode<T> treeNode : this.subTree) {
					long m = distanceFunction.getDistance(treeNode, otherParameters);
					if (l > m) {
						TreeLeafNode<T> treeLeafNode2 = treeNode.getResultingNode(otherParameters, treeLeafNode, distanceFunction);
						long n = treeNode == treeLeafNode2 ? m : distanceFunction.getDistance(treeLeafNode2, otherParameters);
						if (l > n) {
							l = n;
							treeLeafNode = treeLeafNode2;
						}
					}
				}

				return treeLeafNode;
			}
		}

		static final class TreeLeafNode<T> extends TreeNode<T> {
			final T value;

			TreeLeafNode(NoiseHypercube parameters, T value) {
				super(parameters.getParameters());
				this.value = value;
			}

			@Override
			protected TreeLeafNode<T> getResultingNode(
				long[] otherParameters, @Nullable MultiNoiseUtil.SearchTree.TreeLeafNode<T> alternative, NodeDistanceFunction<T> distanceFunction
			) {
				return this;
			}
		}

		abstract static class TreeNode<T> {
			protected final ParameterRange[] parameters;

			protected TreeNode(List<ParameterRange> parameters) {
				this.parameters = (ParameterRange[])parameters.toArray(new ParameterRange[0]);
			}

			protected abstract TreeLeafNode<T> getResultingNode(
				long[] otherParameters, @Nullable MultiNoiseUtil.SearchTree.TreeLeafNode<T> alternative, NodeDistanceFunction<T> distanceFunction
			);

			protected long getSquaredDistance(long[] otherParameters) {
				long l = 0L;

				for (int i = 0; i < 7; i++) {
					l += MathHelper.square(this.parameters[i].getDistance(otherParameters[i]));
				}

				return l;
			}

			public String toString() {
				return Arrays.toString(this.parameters);
			}
		}
	}
}
