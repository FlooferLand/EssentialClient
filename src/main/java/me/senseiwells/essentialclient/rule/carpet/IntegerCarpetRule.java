package me.senseiwells.essentialclient.rule.carpet;

import com.google.gson.JsonElement;
import me.senseiwells.arucas.utils.ExceptionUtils;

public class IntegerCarpetRule extends NumberCarpetRule<Integer> {
	public IntegerCarpetRule(String name, String description, Integer defaultValue) {
		super(name, description, defaultValue);
	}

	@Override
	public Type getType() {
		return Type.INTEGER;
	}

	@Override
	public Integer fromJson(JsonElement element) {
		return element.getAsInt();
	}

	@Override
	public CarpetClientRule<Integer> shallowCopy() {
		return new IntegerCarpetRule(this.getName(), this.getDescription(), this.getDefaultValue());
	}

	@Override
	public Integer getValueFromString(String value) {
		Integer intValue = ExceptionUtils.catchAsNull(() -> Integer.parseInt(value));
		if (intValue == null) {
			this.logCannotSet(value);
			return null;
		}
		return intValue;
	}
}
