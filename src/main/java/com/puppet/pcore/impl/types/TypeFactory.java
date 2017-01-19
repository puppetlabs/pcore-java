package com.puppet.pcore.impl.types;

import com.puppet.pcore.parser.Expression;
import com.puppet.pcore.semver.VersionRange;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;
import static java.util.Arrays.asList;

@SuppressWarnings({"unused", "WeakerAccess" })
public class TypeFactory {

	private TypeFactory() {
	}

	public static CallableType allCallableType() {
		return CallableType.ALL;
	}

	public static AnyType anyType() {
		return AnyType.DEFAULT;
	}

	public static ArrayType arrayType() {
		return ArrayType.DEFAULT;
	}

	public static ArrayType arrayType(AnyType elementType) {
		return arrayType(elementType, IntegerType.POSITIVE);
	}

	public static ArrayType arrayType(AnyType elementType, long min, long max) {
		return arrayType(elementType, integerType(min, max));
	}

	public static ArrayType arrayType(AnyType elementType, IntegerType size) {
		return AnyType.DEFAULT.equals(elementType) && IntegerType.POSITIVE.equals(size)
				? ArrayType.DEFAULT
				: new ArrayType(elementType, size);
	}

	public static BinaryType binaryType() {
		return BinaryType.DEFAULT;
	}

	public static BooleanType booleanType() {
		return BooleanType.DEFAULT;
	}

	public static CallableType callableType() {
		return callableType(TupleType.DEFAULT, null);
	}

	public static CallableType callableType(TupleType parametersType) {
		return callableType(parametersType, null);
	}

	public static CallableType callableType(TupleType parametersType, CallableType blockType) {
		return callableType(parametersType, blockType, AnyType.DEFAULT);
	}

	public static CallableType callableType(TupleType parametersType, CallableType blockType, AnyType returnType) {
		return parametersType.equals(TupleType.DEFAULT) && blockType == null && returnType.equals(AnyType.DEFAULT)
				? CallableType.DEFAULT
				: new CallableType(parametersType, blockType, returnType);
	}

	public static CatalogEntryType catalogEntryType() {
		return CatalogEntryType.DEFAULT;
	}

	public static ClassType classType() {
		return ClassType.DEFAULT;
	}

	public static ClassType classType(String className) {
		return className == null ? ClassType.DEFAULT : new ClassType(className);
	}

	public static CollectionType collectionType() {
		return CollectionType.DEFAULT;
	}

	public static CollectionType collectionType(long min, long max) {
		return collectionType(integerType(min, max));
	}

	public static CollectionType collectionType(IntegerType sizeType) {
		return IntegerType.POSITIVE.equals(sizeType)
				? CollectionType.DEFAULT
				: new CollectionType(anyType(), sizeType);
	}

	public static DataType dataType() {
		return DataType.DEFAULT;
	}

	public static DefaultType defaultType() {
		return DefaultType.DEFAULT;
	}

	public static EnumType enumType() {
		return EnumType.DEFAULT;
	}

	public static EnumType enumType(String... enums) {
		return enums.length == 0 ? EnumType.DEFAULT : new EnumType(unmodifiableCopy(enums));
	}

	public static EnumType enumType(List<String> enums) {
		return enums.isEmpty() ? EnumType.DEFAULT : new EnumType(unmodifiableCopy(enums));
	}

	public static FloatType floatType() {
		return FloatType.DEFAULT;
	}

	public static FloatType floatType(double min) {
		return floatType(min, Double.POSITIVE_INFINITY);
	}

	public static FloatType floatType(double min, double max) {
		return min == Double.NEGATIVE_INFINITY && max == Double.POSITIVE_INFINITY
				? FloatType.DEFAULT
				: new FloatType(min, max);
	}

	public static HashType hashType() {
		return HashType.DEFAULT;
	}

	public static HashType hashType(AnyType keyType, AnyType valueType) {
		return hashType(keyType, valueType, IntegerType.POSITIVE);
	}

	public static HashType hashType(AnyType keyType, AnyType valueType, long min, long max) {
		return hashType(keyType, valueType, integerType(min, max));
	}

	public static HashType hashType(AnyType keyType, AnyType valueType, IntegerType size) {
		return AnyType.DEFAULT.equals(keyType) && AnyType.DEFAULT.equals(valueType) && IntegerType.POSITIVE.equals(size)
				? HashType.DEFAULT
				: new HashType(keyType, valueType, size);
	}

	public static AnyType infer(Object value) {
		return TypeCalculator.SINGLETON.infer(value);
	}

	public static AnyType inferSet(Object value) {
		return TypeCalculator.SINGLETON.inferSet(value);
	}

	public static IntegerType integerType() {
		return IntegerType.DEFAULT;
	}

	public static IntegerType integerType(long min) {
		return integerType(min, Long.MAX_VALUE);
	}

	public static IntegerType integerType(long min, long max) {
		if(min == Long.MIN_VALUE)
			return max == Long.MAX_VALUE ? IntegerType.DEFAULT : new IntegerType(min, max);
		if(min == 0 && max == Long.MAX_VALUE)
			return IntegerType.POSITIVE;
		return new IntegerType(min, max);
	}

	public static IterableType iterableType() {
		return IterableType.DEFAULT;
	}

	public static IterableType iterableType(AnyType type) {
		return AnyType.DEFAULT.equals(type) ? IterableType.DEFAULT : new IterableType(type);
	}

	public static IteratorType iteratorType() {
		return IteratorType.DEFAULT;
	}

	public static IteratorType iteratorType(AnyType type) {
		return AnyType.DEFAULT.equals(type) ? IteratorType.DEFAULT : new IteratorType(type);
	}

	public static NotUndefType notUndefType() {
		return NotUndefType.DEFAULT;
	}

	public static NotUndefType notUndefType(AnyType type) {
		return AnyType.DEFAULT.equals(type) ? NotUndefType.DEFAULT : new NotUndefType(type);
	}

	public static NotUndefType notUndefType(String string) {
		return new NotUndefType(stringType(string));
	}

	public static NumericType numericType() {
		return NumericType.DEFAULT;
	}

	public static ObjectType objectType() {
		return ObjectType.DEFAULT;
	}

	public static ObjectType objectType(Map<String,Object> i12nHash) {
		return new ObjectType(i12nHash);
	}

	public static ObjectType objectType(String name, Expression i12nExpression) {
		return new ObjectType(name, i12nExpression);
	}

	public static OptionalType optionalType() {
		return OptionalType.DEFAULT;
	}

	public static OptionalType optionalType(AnyType type) {
		return AnyType.DEFAULT.equals(type) ? OptionalType.DEFAULT : new OptionalType(type);
	}

	public static OptionalType optionalType(String string) {
		return new OptionalType(stringType(string));
	}

	public static PatternType patternType() {
		return PatternType.DEFAULT;
	}

	public static PatternType patternType(RegexpType... regexps) {
		return regexps.length == 0 ? PatternType.DEFAULT : new PatternType(unmodifiableCopy(regexps));
	}

	public static PatternType patternType(List<RegexpType> regexps) {
		return regexps.isEmpty() ? PatternType.DEFAULT : new PatternType(unmodifiableCopy(regexps));
	}

	public static RegexpType regexpType() {
		return RegexpType.DEFAULT;
	}

	public static RegexpType regexpType(String patternString) {
		return patternString == null || patternString.equals(RegexpType.DEFAULT_PATTERN)
				? RegexpType.DEFAULT
				: new RegexpType(patternString);
	}

	public static RegexpType regexpType(Pattern pattern) {
		return pattern == null ? RegexpType.DEFAULT : new RegexpType(pattern);
	}

	public static ResourceType resourceType() {
		return ResourceType.DEFAULT;
	}

	public static ResourceType resourceType(String typeName) {
		return resourceType(typeName, null);
	}

	public static ResourceType resourceType(String typeName, String title) {
		return typeName == null && title == null ? ResourceType.DEFAULT : new ResourceType(typeName, title);
	}

	public static RuntimeType runtimeType() {
		return RuntimeType.DEFAULT;
	}

	public static RuntimeType runtimeType(String runtimeName, String name) {
		return runtimeType(runtimeName, name, null);
	}

	public static RuntimeType runtimeType(String runtimeName, String name, RegexpType pattern) {
		return runtimeName == null && name == null && pattern == null
				? RuntimeType.DEFAULT
				: new RuntimeType(runtimeName, name, pattern);
	}

	public static ScalarDataType scalarDataType() {
		return ScalarDataType.DEFAULT;
	}

	public static ScalarType scalarType() {
		return ScalarType.DEFAULT;
	}

	public static SemVerRangeType semVerRangeType() {
		return SemVerRangeType.DEFAULT;
	}

	public static SemVerType semVerType() {
		return SemVerType.DEFAULT;
	}

	public static SemVerType semVerType(VersionRange... ranges) {
		return ranges.length == 0 ? SemVerType.DEFAULT : new SemVerType(unmodifiableCopy(ranges));
	}

	public static SemVerType semVerType(List<VersionRange> ranges) {
		return ranges.isEmpty() ? SemVerType.DEFAULT : new SemVerType(unmodifiableCopy(ranges));
	}

	public static SensitiveType sensitiveType() {
		return SensitiveType.DEFAULT;
	}

	public static SensitiveType sensitiveType(AnyType type) {
		return AnyType.DEFAULT.equals(type) ? SensitiveType.DEFAULT : new SensitiveType(type);
	}

	public static StringType stringType() {
		return StringType.DEFAULT;
	}

	public static StringType stringType(long min, long max) {
		return stringType(integerType(min, max));
	}

	public static StringType stringType(Object arg) {
		if(arg instanceof IntegerType)
			return stringType((IntegerType)arg);
		if(arg == null || arg instanceof String)
			return stringType((String)arg);
		throw new IllegalArgumentException("Unable to create a String with an argument of class " + arg.getClass().getName
				());
	}

	public static StringType stringType(IntegerType size) {
		return IntegerType.POSITIVE.equals(size) ? StringType.DEFAULT : new StringType(size);
	}

	public static StringType stringType(String value) {
		return value == null ? StringType.DEFAULT : new StringType(value);
	}

	public static StructElement structElement(String key, AnyType valueType) {
		return new StructElement(key, valueType);
	}

	public static StructElement structElement(AnyType key, AnyType valueType) {
		return new StructElement(key, valueType);
	}

	public static StructType structType() {
		return StructType.DEFAULT;
	}

	public static StructType structType(StructElement... elements) {
		return elements.length == 0 ? StructType.DEFAULT : new StructType(unmodifiableCopy(elements));
	}

	public static StructType structType(List<StructElement> elements) {
		return elements.isEmpty() ? StructType.DEFAULT : new StructType(unmodifiableCopy(elements));
	}

	public static TimeSpanType timeSpanType() {
		return TimeSpanType.DEFAULT;
	}

	public static TimeSpanType timeSpanType(Duration min) {
		return timeSpanType(min, TimeSpanType.MAX_DURATION);
	}

	public static TimeSpanType timeSpanType(Duration min, Duration max) {
		return new TimeSpanType(min, max);
	}

	public static TimestampType timestampType() {
		return TimestampType.DEFAULT;
	}

	public static TimestampType timestampType(Instant min) {
		return timestampType(min, Instant.MAX);
	}

	public static TimestampType timestampType(Instant min, Instant max) {
		return new TimestampType(min, max);
	}

	public static TupleType tupleType() {
		return TupleType.DEFAULT;
	}

	public static TupleType tupleType(List<AnyType> types) {
		return tupleType(types, null);
	}

	public static TupleType tupleType(List<AnyType> types, long min, long max) {
		return tupleType(types, integerType(min, max));
	}

	public static TupleType tupleType(List<AnyType> types, IntegerType size) {
		if(types.isEmpty()) {
			if(size == null || IntegerType.ZERO_SIZE.equals(size))
				return TupleType.EXPLICIT_EMPTY;
			if(IntegerType.POSITIVE.equals(size))
				return TupleType.DEFAULT;
		}
		return new TupleType(unmodifiableCopy(types), size);
	}

	public static TypeAliasType typeAliasType() {
		return TypeAliasType.DEFAULT;
	}

	public static TypeAliasType typeAliasType(String name, Expression typeExpression) {
		return new TypeAliasType(name, typeExpression, null);
	}

	public static TypeAliasType typeAliasType(String name, TypeReferenceType typeExpression) {
		return new TypeAliasType(name, typeExpression, null);
	}

	public static TypeReferenceType typeReferenceType() {
		return TypeReferenceType.DEFAULT;
	}

	public static TypeReferenceType typeReferenceType(String typeString) {
		return new TypeReferenceType(typeString);
	}

	public static TypeSetType typeSetType() {
		return TypeSetType.DEFAULT;
	}

	public static TypeSetType typeSetType(Map<String,Object> i12nHash) {
		return new TypeSetType(i12nHash);
	}

	public static TypeSetType typeSetType(String name, URI nameAuthority, Expression i12nExpression) {
		return new TypeSetType(name, nameAuthority, i12nExpression);
	}

	public static TypeType typeType() {
		return TypeType.DEFAULT;
	}

	public static TypeType typeType(AnyType type) {
		return AnyType.DEFAULT.equals(type) ? TypeType.DEFAULT : new TypeType(type);
	}

	public static UndefType undefType() {
		return UndefType.DEFAULT;
	}

	public static UnitType unitType() {
		return UnitType.DEFAULT;
	}

	public static AnyType variantType(AnyType... types) {
		switch(types.length) {
		case 0:
			return VariantType.DEFAULT;
		case 1:
			return types[0];
		default:
			return variantType(asList(types));
		}
	}

	public static AnyType variantType(List<AnyType> types) {
		types = VariantType.normalize(types);
		switch(types.size()) {
		case 0:
			return VariantType.DEFAULT;
		case 1:
			return types.get(0);
		default:
			return new VariantType(unmodifiableCopy(types));
		}
	}
}
