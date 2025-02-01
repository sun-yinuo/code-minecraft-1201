package net.minecraft.loot.entry;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.loot.condition.LootCondition;

public class SequenceEntry extends CombinedEntry {
	SequenceEntry(LootPoolEntry[] lootPoolEntrys, LootCondition[] lootConditions) {
		super(lootPoolEntrys, lootConditions);
	}

	@Override
	public LootPoolEntryType getType() {
		return LootPoolEntryTypes.SEQUENCE;
	}

	@Override
	protected EntryCombiner combine(EntryCombiner[] children) {
		switch (children.length) {
			case 0:
				return ALWAYS_TRUE;
			case 1:
				return children[0];
			case 2:
				return children[0].and(children[1]);
			default:
				return (context, lootChoiceExpander) -> {
					for (EntryCombiner entryCombiner : children) {
						if (!entryCombiner.expand(context, lootChoiceExpander)) {
							return false;
						}
					}

					return true;
				};
		}
	}

	public static Builder create(LootPoolEntry.Builder<?>... entries) {
		return new Builder(entries);
	}

	public static class Builder extends LootPoolEntry.Builder<Builder> {
		private final List<LootPoolEntry> entries = Lists.<LootPoolEntry>newArrayList();

		public Builder(LootPoolEntry.Builder<?>... entries) {
			for (LootPoolEntry.Builder<?> builder : entries) {
				this.entries.add(builder.build());
			}
		}

		protected Builder getThisBuilder() {
			return this;
		}

		@Override
		public Builder groupEntry(LootPoolEntry.Builder<?> entry) {
			this.entries.add(entry.build());
			return this;
		}

		@Override
		public LootPoolEntry build() {
			return new SequenceEntry((LootPoolEntry[])this.entries.toArray(new LootPoolEntry[0]), this.getConditions());
		}
	}
}
