package net.minecraft.loot.provider.score;

import java.util.Set;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import org.jetbrains.annotations.Nullable;

public interface LootScoreProvider {
	@Nullable
	String getName(LootContext context);

	LootScoreProviderType getType();

	Set<LootContextParameter<?>> getRequiredParameters();
}
