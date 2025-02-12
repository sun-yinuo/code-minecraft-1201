package net.minecraft.advancement;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class AdvancementManager {
	private static final Logger LOGGER = LogUtils.getLogger();
	private final Map<Identifier, Advancement> advancements = Maps.<Identifier, Advancement>newHashMap();
	private final Set<Advancement> roots = Sets.<Advancement>newLinkedHashSet();
	private final Set<Advancement> dependents = Sets.<Advancement>newLinkedHashSet();
	@Nullable
	private AdvancementManager.Listener listener;

	private void remove(Advancement advancement) {
		for (Advancement advancement2 : advancement.getChildren()) {
			this.remove(advancement2);
		}

		LOGGER.info("Forgot about advancement {}", advancement.getId());
		this.advancements.remove(advancement.getId());
		if (advancement.getParent() == null) {
			this.roots.remove(advancement);
			if (this.listener != null) {
				this.listener.onRootRemoved(advancement);
			}
		} else {
			this.dependents.remove(advancement);
			if (this.listener != null) {
				this.listener.onDependentRemoved(advancement);
			}
		}
	}

	public void removeAll(Set<Identifier> advancements) {
		for (Identifier identifier : advancements) {
			Advancement advancement = (Advancement)this.advancements.get(identifier);
			if (advancement == null) {
				LOGGER.warn("Told to remove advancement {} but I don't know what that is", identifier);
			} else {
				this.remove(advancement);
			}
		}
	}

	public void load(Map<Identifier, Advancement.Builder> advancements) {
		Map<Identifier, Advancement.Builder> map = Maps.<Identifier, Advancement.Builder>newHashMap(advancements);

		while (!map.isEmpty()) {
			boolean bl = false;
			Iterator<Entry<Identifier, Advancement.Builder>> iterator = map.entrySet().iterator();

			while (iterator.hasNext()) {
				Entry<Identifier, Advancement.Builder> entry = (Entry<Identifier, Advancement.Builder>)iterator.next();
				Identifier identifier = (Identifier)entry.getKey();
				Advancement.Builder builder = (Advancement.Builder)entry.getValue();
				if (builder.findParent(this.advancements::get)) {
					Advancement advancement = builder.build(identifier);
					this.advancements.put(identifier, advancement);
					bl = true;
					iterator.remove();
					if (advancement.getParent() == null) {
						this.roots.add(advancement);
						if (this.listener != null) {
							this.listener.onRootAdded(advancement);
						}
					} else {
						this.dependents.add(advancement);
						if (this.listener != null) {
							this.listener.onDependentAdded(advancement);
						}
					}
				}
			}

			if (!bl) {
				for (Entry<Identifier, Advancement.Builder> entry : map.entrySet()) {
					LOGGER.error("Couldn't load advancement {}: {}", entry.getKey(), entry.getValue());
				}
				break;
			}
		}

		LOGGER.info("Loaded {} advancements", this.advancements.size());
	}

	public void clear() {
		this.advancements.clear();
		this.roots.clear();
		this.dependents.clear();
		if (this.listener != null) {
			this.listener.onClear();
		}
	}

	public Iterable<Advancement> getRoots() {
		return this.roots;
	}

	public Collection<Advancement> getAdvancements() {
		return this.advancements.values();
	}

	@Nullable
	public Advancement get(Identifier id) {
		return (Advancement)this.advancements.get(id);
	}

	public void setListener(@Nullable AdvancementManager.Listener listener) {
		this.listener = listener;
		if (listener != null) {
			for (Advancement advancement : this.roots) {
				listener.onRootAdded(advancement);
			}

			for (Advancement advancement : this.dependents) {
				listener.onDependentAdded(advancement);
			}
		}
	}

	public interface Listener {
		void onRootAdded(Advancement root);

		void onRootRemoved(Advancement root);

		void onDependentAdded(Advancement dependent);

		void onDependentRemoved(Advancement dependent);

		void onClear();
	}
}
