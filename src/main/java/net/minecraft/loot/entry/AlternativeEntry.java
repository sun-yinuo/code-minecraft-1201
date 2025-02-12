package net.minecraft.loot.entry;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import org.apache.commons.lang3.ArrayUtils;

public class AlternativeEntry extends CombinedEntry {
	AlternativeEntry(LootPoolEntry[] lootPoolEntrys, LootCondition[] lootConditions) {
		super(lootPoolEntrys, lootConditions);
	}

	@Override
	public LootPoolEntryType getType() {
		return LootPoolEntryTypes.ALTERNATIVES;
	}

	@Override
	protected EntryCombiner combine(EntryCombiner[] children) {
		switch (children.length) {
			case 0:
				return ALWAYS_FALSE;
			case 1:
				return children[0];
			case 2:
				return children[0].or(children[1]);
			default:
				return (context, lootChoiceExpander) -> {
					for (EntryCombiner entryCombiner : children) {
						if (entryCombiner.expand(context, lootChoiceExpander)) {
							return true;
						}
					}

					return false;
				};
		}
	}

	@Override
	public void validate(LootTableReporter reporter) {
		super.validate(reporter);

		for (int i = 0; i < this.children.length - 1; i++) {
			if (ArrayUtils.isEmpty((Object[])this.children[i].conditions)) {
				reporter.report("Unreachable entry!");
			}
		}
	}

	public static Builder builder(LootPoolEntry.Builder<?>... children) {
		return new Builder(children);
	}

	public static <E> Builder builder(Collection<E> children, Function<E, LootPoolEntry.Builder<?>> toBuilderFunction) {
		return new Builder((LootPoolEntry.Builder<?>[])children.stream().map(toBuilderFunction::apply).toArray(LootPoolEntry.Builder[]::new));
	}

	public static class Builder extends LootPoolEntry.Builder<Builder> {
		private final List<LootPoolEntry> children = Lists.<LootPoolEntry>newArrayList();

		public Builder(LootPoolEntry.Builder<?>... children) {
			for (LootPoolEntry.Builder<?> builder : children) {
				this.children.add(builder.build());
			}
		}

		protected Builder getThisBuilder() {
			return this;
		}

		@Override
		public Builder alternatively(LootPoolEntry.Builder<?> builder) {
			this.children.add(builder.build());
			return this;
		}

		@Override
		public LootPoolEntry build() {
			return new AlternativeEntry((LootPoolEntry[])this.children.toArray(new LootPoolEntry[0]), this.getConditions());
		}
	}
}
