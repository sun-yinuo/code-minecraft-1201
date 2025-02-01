package net.minecraft.client.item;

public interface TooltipContext {
	Default BASIC = new Default(false, false);
	Default ADVANCED = new Default(true, false);

	boolean isAdvanced();

	boolean isCreative();

	public static record Default(boolean advanced, boolean creative) implements TooltipContext {
		@Override
		public boolean isAdvanced() {
			return this.advanced;
		}

		@Override
		public boolean isCreative() {
			return this.creative;
		}

		public Default withCreative() {
			return new Default(this.advanced, true);
		}
	}
}
