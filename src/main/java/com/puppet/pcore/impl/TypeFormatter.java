package com.puppet.pcore.impl;

import com.puppet.pcore.Binary;
import com.puppet.pcore.impl.types.*;
import com.puppet.pcore.semver.Version;
import com.puppet.pcore.semver.VersionRange;
import com.puppet.pcore.time.DurationFormat;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.puppet.pcore.impl.Constants.KEY_REFERENCES;
import static com.puppet.pcore.impl.Constants.KEY_TYPES;
import static com.puppet.pcore.impl.Helpers.*;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@SuppressWarnings("unused")
public class TypeFormatter extends Polymorphic<Void> {

	interface ArgsAppender {
		void append();
	}

	private static final DispatchMap dispatchMap = initPolymorphicDispatch(TypeFormatter.class, "_format");
	private static final String COMMA_SEP = ", ";
	private static final String HASH_ENTRY_OP = " => ";
	private final boolean debug;
	private final StringBuilder out;
	private boolean expanded;
	private IdentityHashMap<Object,Object> guard;
	private int indent;
	private int indentWidth;
	private TypeSetType typeSet;

	public TypeFormatter(StringBuilder out) {
		this(out, -1, 0, false, false);
	}

	public TypeFormatter(StringBuilder out, boolean aliasExpanded) {
		this(out, -1, 0, aliasExpanded, false);
	}

	public TypeFormatter(StringBuilder out, int indent, int indentWidth, boolean aliasExpanded, boolean debug) {
		this.out = out;
		this.expanded = aliasExpanded;
		this.debug = debug;
		this.indent = indent;
		this.indentWidth = indentWidth;
		if(indent > 0)
			indent();
	}

	public void format(Object value) {
		try {
			dispatch(value);
		} catch(InvocationTargetException e) {
			Throwable te = e.getCause();
			if(!(te instanceof RuntimeException))
				te = new RuntimeException(te);
			throw (RuntimeException)te;
		}
	}

	@Override
	protected DispatchMap getDispatchMap() {
		return dispatchMap;
	}

	void _format(AnyType t) {
		out.append("Any");
	}

	void _format(ArrayType t) {
		if(t.equals((ArrayType.EMPTY)))
			appendArray("Array", () -> appendValues(false, 0, 0));
		else
			appendArray("Array", t.equals(ArrayType.DEFAULT), () -> {
				appendValues(true, t.type);
				appendFormatted(true, rangeArrayPart(t.size, true));
				chompList();
			});
	}

	void _format(BinaryType t) {
		out.append("Binary");
	}

	void _format(BooleanType t) {
		out.append("Boolean");
	}

	void _format(CallableType t) {
		if(AnyType.DEFAULT.equals(t.returnType))
			appendArray("Callable", TupleType.DEFAULT.equals(t.parametersType), () -> appendCallableParams(t));
		else {
			appendArray("Callable", () -> {
				appendArray("", () -> appendCallableParams(t));
				out.append(COMMA_SEP);
				appendValues(false, t.returnType);
			});
		}
	}

	void _format(CatalogEntryType t) {
		out.append("CatalogEntry");
	}

	void _format(ClassType t) {
		appendArray("Class", t.className == null, () -> appendValues(false, singletonList(t.className)));
	}

	void _format(CollectionType t) {
		List<String> range = rangeArrayPart(t.size, true);
		appendArray("Collection", range.isEmpty(), () -> appendFormatted(false, range));
	}

	void _format(DataType t) {
		out.append("Data");
	}

	void _format(DefaultType t) {
		out.append("Default");
	}

	void _format(EnumType t) {
		appendArray("Enum", t.enums.isEmpty(), () -> appendValues(false, t.enums));
	}

	void _format(FloatType t) {
		appendArray("Float", t.isUnbounded(), () -> appendFormatted(false, rangeArrayPart(t)));
	}

	void _format(Map<Object,Object> m) {
		appendHash(m, null, null);
	}

	void _format(Void undef) {
		out.append("?");
	}

	void _format(Boolean value) {
		out.append(value.toString());
	}

	void _format(Character value) {
		_format(value.toString());
	}

	void _format(Float value) {
		out.append(value.toString());
	}

	void _format(Double value) {
		out.append(value.toString());
	}

	void _format(Version value) {
		_format(value.toString());
	}

	void _format(VersionRange value) {
		_format(value.toString());
	}

	void _format(Duration value) {
		_format(DurationFormat.DEFAULTS.get(0).format(value));
	}

	void _format(Instant value) {
		// TODO: Instant parsing and formatting
		_format(value.toString());
	}

	void _format(Binary value) {
		_format(value.toString());
	}

	void _format(Pattern value) {
		puppetRegexp(value.pattern(), out);
	}

	void _format(HashType t) {
		if(HashType.EMPTY.equals(t))
			appendArray("Hash", () -> appendValues(false, 0, 0));
		else
			appendArray("Hash", HashType.DEFAULT.equals(t), () -> {
				appendValues(true, t.keyType, t.type);
				appendFormatted(true, rangeArrayPart(t.size, true));
				chompList();
			});
	}

	void _format(Byte b) {
		out.append(b.toString());
	}

	void _format(Short s) {
		out.append(s.toString());
	}

	void _format(Integer i) {
		out.append(i.toString());
	}

	void _format(Long l) {
		out.append(l.toString());
	}

	void _format(IntegerType t) {
		appendArray("Integer", t.isUnbounded(), () -> appendFormatted(false, rangeArrayPart(t, false)));
	}

	void _format(IterableType t) {
		appendArray("Iterable", AnyType.DEFAULT.equals(t.type), () -> format(t.type));
	}

	void _format(IteratorType t) {
		appendArray("Iterator", AnyType.DEFAULT.equals(t.type), () -> format(t.type));
	}

	void _format(List<?> list) {
		appendArray("", () -> appendValues(false, list));
	}

	void _format(NotUndefType t) {
		appendArray("NotUndef", AnyType.DEFAULT.equals(t.type), () -> {
			AnyType tt = t.type;
			format(tt instanceof StringType && ((StringType)tt).value != null ? ((StringType)tt).value : tt);
		});
	}

	void _format(NumericType t) {
		out.append("Numeric");
	}

	void _format(ObjectType t) {
		if(expanded)
			appendObjectHash(t.i12nHash(typeSet == null || !typeSet.definesType(t)));
		else {
			if(typeSet != null)
				out.append(typeSet.nameFor(t));
			else
				out.append(t.name() == null ? "Object" : t.name());
		}
	}

	void _format(OptionalType t) {
		appendArray("Optional", AnyType.DEFAULT.equals(t.type), () -> {
			AnyType tt = t.type;
			format(tt instanceof StringType && ((StringType)tt).value != null ? ((StringType)tt).value : tt);
		});
	}

	void _format(PatternType t) {
		appendArray("Pattern", t.regexps.isEmpty(), () -> {
			for(RegexpType rx : t.regexps) {
				puppetRegexp(rx.patternString, out);
				out.append(COMMA_SEP);
			}
			chompList();
		});
	}

	void _format(RegexpType t) {
		appendArray("Regexp", RegexpType.DEFAULT_PATTERN.equals(t.patternString), () -> puppetRegexp(
				t.patternString,
				out));
	}

	void _format(ResourceType t) {
		if(t.typeName == null)
			out.append("Resource");
		else
			appendArray(capitalizeSegments(t.typeName), t.title == null, () -> format(t.title));
	}

	void _format(RuntimeType t) {
		Object nameOrPattern = t.pattern == null ? t.name : asList(t.pattern, t.name);
		appendArray("Runtime", t.runtime == null, () -> appendValues(false, t.runtime, nameOrPattern));
	}

	void _format(ScalarType t) {
		out.append("Scalar");
	}

	void _format(ScalarDataType t) {
		out.append("ScalarData");
	}

	void _format(SemVerRangeType t) {
		out.append("SemVerRange");
	}

	void _format(SemVerType t) {
		appendArray("SemVer", t.ranges.isEmpty(), () -> appendValues(false, t.ranges));
	}

	void _format(SensitiveType t) {
		appendArray("Sensitive", AnyType.DEFAULT.equals(t.type), () -> format(t.type));
	}

	void _format(String s) {
		puppetQuote(s, out);
	}

	void _format(StringType t) {
		List<String> range = rangeArrayPart(t.size, true);
		if(debug && t.value != null)
			appendArray("String", () -> format(t.value));
		else
			appendArray("String", range.isEmpty() || t.value != null, () -> appendFormatted(false, range));
	}

	void _format(StructType t) {
		appendArray("Struct", t.elements.isEmpty(), () -> {
			Map<Object,Object> memberHash = new LinkedHashMap<>();
			for(StructElement element : t.elements)
				addStructMember(memberHash, element);
			appendHash(memberHash, null, null);
		});
	}

	void _format(TimeSpanType t) {
		appendArray("TimeSpan", t.isUnbounded(), () -> {
			if(TimeSpanType.MIN_DURATION.equals(t.min))
				out.append("default");
			else
				format(t.min);
			if(!(TimeSpanType.MAX_DURATION.equals(t.max) || t.min.equals(t.max))) {
				out.append(COMMA_SEP);
				format(t.max);
			}
		});
	}

	void _format(TimestampType t) {
		appendArray("Timestamp", t.isUnbounded(), () -> {
			if(Instant.MIN.equals(t.min))
				out.append("default");
			else
				format(t.min);
			if(!(Instant.MAX.equals(t.max) || t.min.equals(t.max))) {
				out.append(COMMA_SEP);
				format(t.max);
			}
		});
	}

	void _format(TupleType t) {
		appendArray("Tuple", t.types.isEmpty(), () -> {
			appendValues(true, t.types);
			appendFormatted(true, rangeArrayPart(t.size, true));
			chompList();
		});
	}

	void _format(TypeAliasType t) {
		if(t.equals(typeAliasType())) {
			out.append("TypeAlias");
			return;
		}

		boolean expand = expanded;
		if(expand) {
			if(guard == null)
				guard = new IdentityHashMap<>();
			expand = guard.put(t, t) == null;
		}
		if(typeSet == null) {
			out.append(t.name);
			if(expand) {
				out.append(" = ");
				appendValues(false, t.resolvedType());
			}
		} else {
			if(expand && typeSet.definesType(t))
				appendValues(false, t.resolvedType());
			else
				out.append(typeSet.nameFor(t));
		}
	}

	void _format(TypeReferenceType t) {
		appendArray("TypeReference", t == typeReferenceType(), () -> appendValues(false, t.typeString));
	}

	@SuppressWarnings("unchecked")
	void _format(TypeSetType t) {
		appendArray("TypeSet", () -> appendHash(t.i12nHash(), (k) -> out.append(symbolicKey(k)), (e) -> {
			switch(e.getKey()) {
			case KEY_TYPES:
				TypeSetType saveTS = typeSet;
				typeSet = t;
				try {
					appendHash((Map<String,Object>)e.getValue(), (tk) -> out.append(symbolicKey(tk)), (te) -> {
						if(te.getValue() instanceof Map<?,?>)
							appendObjectHash((Map<String,Object>)te.getValue());
						else
							appendValues(false, te.getValue());
					});
				} finally {
					typeSet = saveTS;
				}
				break;
			case KEY_REFERENCES:
				appendHash((Map<String,Object>)e.getValue(), (tk) -> out.append(symbolicKey(tk)), (te) ->
						appendHash((Map<String,Object>)te.getValue(), (ttk) -> out.append(symbolicKey(ttk)), null));
				break;
			default:
				appendValues(false, e.getValue());
			}
		}));
	}

	void _format(TypeType t) {
		appendArray("Type", AnyType.DEFAULT.equals(t.type), () -> format(t.type));
	}

	void _format(UndefType t) {
		out.append("Undef");
	}

	void _format(UnitType t) {
		out.append("Unit");
	}

	void _format(VariantType t) {
		appendArray("Variant", t.types.isEmpty(), () -> appendValues(false, t.types));
	}

	private void addStructMember(Map<Object,Object> memberHash, StructElement element) {
		AnyType k = element.key;
		Object ko = k;
		boolean optionalValue = element.value.isAssignable(undefType());
		if(k instanceof OptionalType) {
			if(optionalValue)
				ko = element.name;
		} else {
			if(optionalValue)
				ko = notUndefType(k);
			else
				ko = element.name;
		}
		memberHash.put(ko, element.value);
	}

	private void appendArray(String typeName, ArgsAppender appender) {
		appendArray(typeName, false, appender);
	}

	private void appendArray(String typeName, boolean empty, ArgsAppender appender) {
		out.append(typeName);
		if(!empty) {
			out.append("[");
			appender.append();
			out.append("]");
		}
	}

	private void appendCallableParams(CallableType t) {
		List<AnyType> paramTypes = t.parametersType.types;
		if(paramTypes.isEmpty() && IntegerType.ZERO_SIZE.equals(t.parametersType.size))
			appendValues(true, 0, 0);
		else {
			appendValues(true, filter(paramTypes, pt -> !(pt instanceof UnitType)).toArray());
			appendFormatted(true, rangeArrayPart(t.parametersType.size, false));
		}
		if(t.blockType != null)
			appendValues(true, t.blockType);
		chompList();
	}

	private void appendFormatted(boolean toBeContinued, List<String> values) {
		if(!values.isEmpty()) {
			for(String value : values) {
				out.append(value);
				out.append(COMMA_SEP);
			}
			if(!toBeContinued)
				chompList();
		}
	}

	private <K, V> void appendHash(Map<K,V> hash, Consumer<K> keyProc, Consumer<Map.Entry<K,V>> entryProc) {
		out.append('{');
		if(indent >= 0)
			++indent;
		for(Map.Entry<K,V> entry : hash.entrySet()) {
			if(indent >= 0)
				newline();
			if(keyProc == null)
				format(entry.getKey());
			else
				keyProc.accept(entry.getKey());
			out.append(HASH_ENTRY_OP);
			if(entryProc == null)
				format(entry.getValue());
			else
				entryProc.accept(entry);
			out.append(COMMA_SEP);
		}
		chompList();
		if(indent >= 0) {
			indent--;
			newline();
		}
		out.append('}');
	}

	@SuppressWarnings("unchecked")
	private void appendObjectHash(Map<String,Object> hash) {
		boolean saveExpanded = expanded;
		expanded = false;
		try {
			appendArray("Object", () -> appendHash(hash, (k) -> out.append(symbolicKey(k)), (e) -> {
				switch(e.getKey()) {
				case Constants.KEY_ATTRIBUTES:
				case Constants.KEY_FUNCTIONS:
					appendHash((Map<String,Object>)e.getValue(), null, (fe) -> {
						if(fe.getValue() instanceof Map<?,?>)
							appendHash((Map<String,Object>)fe.getValue(), (fak) -> out.append(symbolicKey(fak)), (fa -> {
								switch(fa.getKey()) {
								case Constants.KEY_KIND:
									out.append(fa.getValue());
									break;
								default:
									appendValues(false, fa.getValue());
								}
							}));
						else
							appendValues(false, fe.getValue());
					});
					break;
				default:
					appendValues(false, e.getValue());
				}
			}));
		} finally {
			expanded = saveExpanded;
		}
	}

	private void appendValues(
			boolean toBeContinued, @SuppressWarnings("TypeParameterExplicitlyExtendsObject") List<?
			extends Object> values) {
		if(!values.isEmpty()) {
			for(Object value : values) {
				format(value);
				out.append(COMMA_SEP);
			}
			if(!toBeContinued)
				chompList();
		}
	}

	private void appendValues(boolean toBeContinued, Object... values) {
		if(values.length > 0) {
			for(Object value : values) {
				format(value);
				out.append(COMMA_SEP);
			}
			if(!toBeContinued)
				chompList();
		}
	}

	private void chompList() {
		int pos = out.length();
		while(--pos >= 0) {
			char c = out.charAt(pos);
			if(!(Character.isWhitespace(c) || c == ','))
				break;
		}
		out.setLength(++pos);
	}

	private void indent() {
		int spaceCount = indent * indentWidth;
		while(--spaceCount >= 0)
			out.append(' ');
	}

	private void newline() {
		int pos = out.length();
		while(--pos >= 0) {
			char c = out.charAt(pos);
			if(c != ' ' && c != '\t')
				break;
		}
		out.setLength(++pos);
		out.append('\n');
		indent();
	}

	private List<String> rangeArrayPart(IntegerType t, boolean skipDefault) {
		return t == null || skipDefault && IntegerType.POSITIVE.equals(t)
				? Collections.emptyList()
				: asList(
						t.min == Long.MIN_VALUE ? "default" : Long.toString(t.min),
						t.max == Long.MAX_VALUE ? "default" : Long.toString(t.max));
	}

	private List<String> rangeArrayPart(FloatType t) {
		return asList(
				t.min == Double.NEGATIVE_INFINITY ? "default" : Double.toString(t.min),
				t.max == Double.POSITIVE_INFINITY ? "default" : Double.toString(t.max));
	}

	private String symbolicKey(String key) {
		return key;
	}
}
