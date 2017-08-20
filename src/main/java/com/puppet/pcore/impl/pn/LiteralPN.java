package com.puppet.pcore.impl.pn;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.puppet.pcore.impl.Helpers.doubleQuote;

public class LiteralPN extends AbstractPN {
	public final Object value;

	public LiteralPN(Object value) {
		this.value = value;
	}

	// Strip zeroes between last significant digit and end or exponent. The
	// zero following the decimal point is considered significant.
	private static final Pattern STRIP_TRAILING_ZEROES = Pattern.compile(
			"\\A(.*(?:\\.0|[1-9]))0+(e[+-]?\\d+)?\\z"
	);

	@Override
	public void format(StringBuilder bld) {
		if(value instanceof String)
			doubleQuote((String)value, bld, false);
		else if(value instanceof Double || value instanceof Float) {
			// We want 16 digit precision that overflows into scientific notation and no trailing zeroes
			String str = String.format("%.16g", value);
			if(str.indexOf('.') < 0 && str.indexOf('e') < 0)
				// %g sometimes yields an integer number without decimals or scientific
				// notation. Scientific notation must then be used to retain type information
				str = String.format("%.16e", value);

			Matcher m = STRIP_TRAILING_ZEROES.matcher(str);
			if(m.matches()) {
				bld.append(m.group(1));
				if(m.group(2) != null)
					bld.append(m.group(2));
			} else
				bld.append(str);
		}
		else
			bld.append(value);
	}

	@Override
	public Object toData() {
		return value;
	}
}
