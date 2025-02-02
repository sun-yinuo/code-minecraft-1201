package net.minecraft.server;

import com.google.gson.JsonObject;
import java.util.Date;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class BannedIpEntry extends BanEntry<String> {
	public BannedIpEntry(String ip) {
		this(ip, null, null, null, null);
	}

	public BannedIpEntry(String ip, @Nullable Date created, @Nullable String source, @Nullable Date expiry, @Nullable String reason) {
		super(ip, created, source, expiry, reason);
	}

	@Override
	public Text toText() {
		return Text.literal(String.valueOf(this.getKey()));
	}

	public BannedIpEntry(JsonObject json) {
		super(getIp(json), json);
	}

	private static String getIp(JsonObject json) {
		return json.has("ip") ? json.get("ip").getAsString() : null;
	}

	@Override
	protected void write(JsonObject json) {
		if (this.getKey() != null) {
			json.addProperty("ip", this.getKey());
			super.write(json);
		}
	}
}
