package com.puppet.pcore.impl.types;

import com.puppet.pcore.Pcore;
import com.puppet.pcore.Type;
import com.puppet.pcore.impl.Helpers;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.impl.SelfReferencingFactoryImpl;
import com.puppet.pcore.parser.Expression;
import com.puppet.pcore.regex.Regexp;
import com.puppet.pcore.semver.VersionRange;
import com.puppet.pcore.serialization.ArgumentsAccessor;
import com.puppet.pcore.serialization.FactoryDispatcher;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.puppet.pcore.impl.ConstructorImpl.hashConstructor;
import static com.puppet.pcore.impl.FactoryDispatcherImpl.dispatcher;
import static com.puppet.pcore.impl.Helpers.map;
import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;
import static com.puppet.pcore.impl.ConstructorImpl.constructor;
import static com.puppet.pcore.impl.types.TypeSetType.TYPE_TYPESET_INIT;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

@SuppressWarnings({"unused", "WeakerAccess" })
public class TypeFactory {
	private TypeFactory() {
	}

	public static CallableType allCallableType() {
		return CallableType.ALL;
	}

	// AnyType
	public static AnyType anyType() {
		return AnyType.DEFAULT;
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<AnyType> anyTypeDispatcher() {
		return dispatcher(constructor(args -> anyType()));
	}

	// ArrayType
	public static ArrayType arrayType() {
		return ArrayType.DEFAULT;
	}

	public static ArrayType arrayTypeEmpty() {
		return ArrayType.EMPTY;
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

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<ArrayType> arrayTypeDispatcher() {
		return dispatcher(
				constructor(args -> arrayType()),
				constructor(args -> arrayType((AnyType)args.get(0)),
						typeType()),
				constructor(args -> arrayType((AnyType)args.get(0), (Long)args.get(1), (Long)args.get(2)),
						typeType(), integerType(), integerType()),
				constructor(args -> arrayType((AnyType)args.get(0), (IntegerType)args.get(1)),
						typeType(), typeType(integerType())),
				constructor((ObjectType)arrayType()._pcoreType())
		);
	}

	// BinaryType
	public static BinaryType binaryType() {
		return BinaryType.DEFAULT;
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<BinaryType> binaryTypeDispatcher() {
		return dispatcher(constructor(args -> binaryType()));
	}

	// BooleanType
	public static BooleanType booleanType() {
		return BooleanType.DEFAULT;
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<BooleanType> booleanTypeDispatcher() {
		return dispatcher(constructor(args -> booleanType()));
	}

	// CallableType
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

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<CallableType> callableTypeDispatcher() {
		return dispatcher(
				constructor(args -> callableType()),
				constructor(args -> callableType((TupleType)args.get(0)),
						typeType(tupleType())),
				constructor(args -> callableType((TupleType)args.get(0), (CallableType)args.get(1)),
						typeType(tupleType()), optionalType(typeType(callableType()))),
				constructor(args -> callableType((TupleType)args.get(0), (CallableType)args.get(1), (AnyType)args.get(2)),
						typeType(tupleType()), optionalType(typeType(callableType())), typeType(anyType())),
				constructor((ObjectType)callableType()._pcoreType())
		);
	}

	// CatalogEntryType
	public static CatalogEntryType catalogEntryType() {
		return CatalogEntryType.DEFAULT;
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<CatalogEntryType> catalogEntryTypeDispatcher() {
		return dispatcher(constructor(args -> catalogEntryType()));
	}

	// ClassType
	public static ClassType classType() {
		return ClassType.DEFAULT;
	}

	public static ClassType classType(String className) {
		return className == null ? ClassType.DEFAULT : new ClassType(className);
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<ClassType> classTypeDispatcher() {
		return dispatcher(
				constructor(args -> classType()),
				constructor(args -> classType((String)args.get(0)),
						stringType()),
				constructor((ObjectType)classType()._pcoreType())
		);
	}

	// CollectionType
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

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<CollectionType> collectionTypeDispatcher() {
		return dispatcher(
				constructor(args -> collectionType()),
				constructor(args -> collectionType((IntegerType)args.get(0)),
						typeType(integerType())),
				constructor((ObjectType)collectionType()._pcoreType())
		);
	}

	// Data (this is an alias), not a specialization of AnyType
	public static AnyType dataType() {
		return ((PcoreImpl)Pcore.staticPcore()).data;
	}

	// RichData (this is an alias), not a specialization of AnyType
	public static AnyType richDataType() {
		return ((PcoreImpl)Pcore.staticPcore()).richData;
	}

	// DefaultType
	public static DefaultType defaultType() {
		return DefaultType.DEFAULT;
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<DefaultType> defaultTypeDispatcher() {
		return dispatcher(constructor(args -> defaultType()));
	}

	// EnumType
	public static EnumType enumType() {
		return EnumType.DEFAULT;
	}

	public static EnumType enumType(String... enums) {
		return enums.length == 0 ? EnumType.DEFAULT : new EnumType(Helpers.asList(enums));
	}

	public static EnumType enumType(List<String> enums) {
		return enums.isEmpty() ? EnumType.DEFAULT : new EnumType(unmodifiableCopy(enums));
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<EnumType> enumTypeDispatcher() {
		return dispatcher(
				constructor(args -> enumType()),
				constructor(args -> enumType((List<String>)args.get(0)),
						arrayType(stringType())),
				constructor((ObjectType)enumType()._pcoreType())
		);
	}

	// ErrorType (this is an ObjectType), not a specialization of AnyType
	public static ObjectType errorType() {
		return ((PcoreImpl)Pcore.staticPcore()).error;
	}

	// FloatType
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

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<FloatType> floatTypeDispatcher() {
		return dispatcher(
				constructor(args -> floatType()),
				constructor(args -> floatType(((Number)args.get(0)).doubleValue()),
						floatType()),
				constructor(args -> floatType(((Number)args.get(0)).doubleValue(), ((Number)args.get(1)).doubleValue()),
						floatType(), floatType()),
				constructor((ObjectType)floatType()._pcoreType())
		);
	}

	// HashType
	public static HashType hashType() {
		return HashType.DEFAULT;
	}

	public static HashType hashTypeEmpty() {
		return HashType.EMPTY;
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

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<HashType> hashTypeDispatcher() {
		return dispatcher(
				constructor(args -> hashType()),
				constructor(args -> hashType((AnyType)args.get(0), (AnyType)args.get(1)),
						typeType(), typeType()),
				constructor(args -> hashType((AnyType)args.get(0), (AnyType)args.get(1), ((Number)args.get(2)).longValue(), ((Number)args.get(3)).longValue()),
						typeType(), typeType(), integerType(), integerType()),
				constructor(args -> hashType((AnyType)args.get(0), (AnyType)args.get(1), (IntegerType)args.get(2)),
						typeType(), typeType(), typeType(integerType())),
				constructor((ObjectType)hashType()._pcoreType())
		);
	}

	public static AnyType infer(Object value) {
		return TypeCalculator.SINGLETON.infer(value);
	}

	public static AnyType inferSet(Object value) {
		return TypeCalculator.SINGLETON.inferSet(value);
	}

	// InitType
	public static InitType initType() {
		return InitType.DEFAULT;
	}

	public static InitType initType(AnyType type) {
		return AnyType.DEFAULT.equals(type) ? InitType.DEFAULT : new InitType(type, emptyList(), false);
	}

	public static InitType initType(AnyType type, List<?> args) {
		return new InitType(type, args, false);
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<InitType> initTypeDispatcher() {
		return dispatcher(
				constructor(args -> initType()),
				constructor(args -> initType((AnyType)args.get(0)),
						typeType()),
				constructor(args -> initType((AnyType)args.get(0), (List<?>)args.get(1)),
						typeType(), arrayType(anyType())),
				constructor((ObjectType)initType()._pcoreType())
		);
	}

	// IntegerType
	public static IntegerType integerType() {
		return IntegerType.DEFAULT;
	}

	public static IntegerType integerTypePositive() {
		return IntegerType.POSITIVE;
	}

	public static IntegerType integerTypeZero() {
		return IntegerType.ZERO_SIZE;
	}

	public static IntegerType integerType(long min) {
		return integerType(min, Long.MAX_VALUE);
	}

	public static IntegerType integerType(long min, long max) {
		if(min == Long.MIN_VALUE)
			return max == Long.MAX_VALUE ? IntegerType.DEFAULT : new IntegerType(min, max);

		if(min == 0) {
			if(max == 0)
				return IntegerType.ZERO_SIZE;
			if(max == Long.MAX_VALUE)
				return IntegerType.POSITIVE;
		}
		return new IntegerType(min, max);
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<IntegerType> integerTypeDispatcher() {
		return dispatcher(
				constructor(args -> integerType()),
				constructor(args -> integerType(((Number)args.get(0)).longValue()),
						integerType()),
				constructor(args -> integerType(((Number)args.get(0)).longValue(), ((Number)args.get(1)).longValue()),
						integerType(), integerType()),
				constructor((ObjectType)integerType()._pcoreType())
		);
	}

	// IterableType
	public static IterableType iterableType() {
		return IterableType.DEFAULT;
	}

	public static IterableType iterableType(AnyType type) {
		return AnyType.DEFAULT.equals(type) ? IterableType.DEFAULT : new IterableType(type);
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<IterableType> iterableTypeDispatcher() {
		return dispatcher(
				constructor(args -> iterableType()),
				constructor(args -> iterableType((AnyType)args.get(0)),
						typeType()),
				constructor((ObjectType)iterableType()._pcoreType())
		);
	}

	// IteratorType
	public static IteratorType iteratorType() {
		return IteratorType.DEFAULT;
	}

	public static IteratorType iteratorType(AnyType type) {
		return AnyType.DEFAULT.equals(type) ? IteratorType.DEFAULT : new IteratorType(type);
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<IteratorType> iteratorTypeDispatcher() {
		return dispatcher(
				constructor(args -> iteratorType()),
				constructor(args -> iteratorType((AnyType)args.get(0)),
						typeType()),
				constructor((ObjectType)iteratorType()._pcoreType())
		);
	}

	// NotUndefType
	public static NotUndefType notUndefType() {
		return NotUndefType.DEFAULT;
	}

	public static NotUndefType notUndefType(AnyType type) {
		return AnyType.DEFAULT.equals(type) ? NotUndefType.DEFAULT : new NotUndefType(type);
	}

	public static NotUndefType notUndefType(String string) {
		return new NotUndefType(stringType(string));
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<NotUndefType> notUndefTypeDispatcher() {
		return dispatcher(
				constructor(args -> notUndefType()),
				constructor(args -> notUndefType((AnyType)args.get(0)),
						typeType()),
				constructor(args -> notUndefType((String)args.get(0)),
						stringType()),
				constructor((ObjectType)notUndefType()._pcoreType())
		);
	}

	// NumericType
	public static NumericType numericType() {
		return NumericType.DEFAULT;
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<NumericType> numericTypeDispatcher() {
		return dispatcher(constructor(args -> numericType()));
	}

	// ObjectType
	public static ObjectType objectType() {
		return ObjectType.DEFAULT;
	}

	public static ObjectType objectType(Map<String,Object> initHash) {
		return new ObjectType(initHash);
	}

	public static ObjectType objectType(String name, Expression initExpression) {
		return new ObjectType(name, initExpression);
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<ObjectType> objectTypeDispatcher() {
		return new SelfReferencingFactoryImpl(asList(
				constructor(args -> objectType()),
				constructor(args -> objectType((String)args.get(0), (Expression)args.get(1)),
						stringType(), runtimeType(Expression.class.getName())),
				hashConstructor(args -> objectType((Map<String,Object>)args.get(0)),
						ObjectType.TYPE_OBJECT_INIT)
		)) {
			@Override
			public ObjectType createInstance(Type type, ArgumentsAccessor aa) throws IOException {
				return new ObjectType(aa);
			}
		};
	}

	// OptionalType
	public static OptionalType optionalType() {
		return OptionalType.DEFAULT;
	}

	public static OptionalType optionalType(AnyType type) {
		return AnyType.DEFAULT.equals(type) ? OptionalType.DEFAULT : new OptionalType(type);
	}

	public static OptionalType optionalType(String string) {
		return new OptionalType(stringType(string));
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<OptionalType> optionalTypeDispatcher() {
		return dispatcher(
				constructor(args -> optionalType()),
				constructor(args -> optionalType((AnyType)args.get(0)),
						typeType()),
				constructor(args -> optionalType((StringType)args.get(0)),
						stringType()),
				constructor((ObjectType)optionalType()._pcoreType())
		);
	}

	// PatternType
	public static PatternType patternType() {
		return PatternType.DEFAULT;
	}

	public static PatternType patternType(Object... regexps) {
		return regexps.length == 0 ? PatternType.DEFAULT : new PatternType(Helpers.asList(regexps));
	}

	public static PatternType patternType(List<Object> regexps) {
		return regexps.isEmpty() ? PatternType.DEFAULT : new PatternType(unmodifiableCopy(regexps));
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<PatternType> patternTypeDispatcher() {
		return dispatcher(
				constructor(args -> patternType()),
				constructor(args -> patternType((List<Object>)args.get(0)),
						arrayType(variantType(stringType(), regexpType(), typeType(regexpType()), typeType(patternType())))),
				constructor((ObjectType)patternType()._pcoreType())
		);
	}

	// RegexpType
	public static RegexpType regexpType() {
		return RegexpType.DEFAULT;
	}

	public static RegexpType regexpType(String patternString) {
		return patternString == null || patternString.equals(RegexpType.DEFAULT_PATTERN)
				? RegexpType.DEFAULT
				: new RegexpType(patternString);
	}

	public static RegexpType regexpType(Regexp pattern) {
		return pattern == null || pattern.toString().equals(RegexpType.DEFAULT_PATTERN)
				? RegexpType.DEFAULT
				: new RegexpType(pattern);
	}

	public static RegexpType regexpType(Object pattern) {
		if(pattern instanceof String)
			return regexpType((String)pattern);
		if(pattern instanceof Regexp)
			return regexpType((Regexp)pattern);
		if(pattern instanceof RegexpType)
			return (RegexpType)pattern;
		throw new IllegalArgumentException(format("Regexp parameter must be a String or a Regexp, got %s", pattern.getClass().getName()));
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<RegexpType> regexpTypeDispatcher() {
		return dispatcher(
				constructor(args -> regexpType()),
				constructor(args -> regexpType((String)args.get(0)),
						stringType()),
				constructor(args -> regexpType((Regexp)args.get(0)),
						regexpType()),
				constructor((ObjectType)regexpType()._pcoreType())
		);
	}

	// ResourceType
	public static ResourceType resourceType() {
		return ResourceType.DEFAULT;
	}

	public static ResourceType resourceType(String typeName) {
		return resourceType(typeName, null);
	}

	public static ResourceType resourceType(String typeName, String title) {
		return typeName == null && title == null ? ResourceType.DEFAULT : new ResourceType(typeName, title);
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<ResourceType> resourceTypeDispatcher() {
		return dispatcher(
				constructor(args -> resourceType()),
				constructor(args -> resourceType((String)args.get(0)),
						stringType()),
				constructor(args -> resourceType((String)args.get(0), (String)args.get(1)),
						stringType(), stringType()),
				constructor((ObjectType)resourceType()._pcoreType())
		);
	}

	// RuntimeType
	public static RuntimeType runtimeType() {
		return RuntimeType.DEFAULT;
	}

	public static RuntimeType runtimeType(String name) {
		return runtimeType("java", name, null);
	}

	public static RuntimeType runtimeType(String runtimeName, String name) {
		return runtimeType(runtimeName, name, null);
	}

	public static RuntimeType runtimeType(String runtimeName, String name, RegexpType pattern) {
		return runtimeName == null && name == null && pattern == null
				? RuntimeType.DEFAULT
				: new RuntimeType(runtimeName, name, pattern);
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<RuntimeType> runtimeTypeDispatcher() {
		return dispatcher(
				constructor(args -> runtimeType()),
				constructor(args -> runtimeType((String)args.get(0)),
						stringType()),
				constructor(args -> runtimeType((String)args.get(0), (String)args.get(1)),
						stringType(), stringType()),
				constructor(args -> runtimeType((String)args.get(0), (String)args.get(1), (RegexpType)args.get(2)),
						stringType(), stringType(), typeType(regexpType())),
				constructor((ObjectType)runtimeType()._pcoreType())
		);
	}

	// ScalarDataType
	public static ScalarDataType scalarDataType() {
		return ScalarDataType.DEFAULT;
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<ScalarDataType> scalarDataTypeDispatcher() {
		return dispatcher(constructor(args -> scalarDataType()));
	}

	// ScalarType
	public static ScalarType scalarType() {
		return ScalarType.DEFAULT;
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<ScalarType> scalarTypeDispatcher() {
		return dispatcher(constructor(args -> scalarType()));
	}

	// SemVerRangeType
	public static SemVerRangeType semVerRangeType() {
		return SemVerRangeType.DEFAULT;
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<SemVerRangeType> semVerRangeTypeDispatcher() {
		return dispatcher(constructor(args -> semVerRangeType()));
	}

	// SemVerType
	public static SemVerType semVerType() {
		return SemVerType.DEFAULT;
	}

	public static SemVerType semVerType(VersionRange... ranges) {
		return ranges.length == 0 ? SemVerType.DEFAULT : new SemVerType(Helpers.asList(ranges));
	}

	public static SemVerType semVerType(List<VersionRange> ranges) {
		return ranges.isEmpty() ? SemVerType.DEFAULT : new SemVerType(unmodifiableCopy(ranges));
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<SemVerType> semVerTypeDispatcher() {
		return dispatcher(
				constructor(args -> semVerType()),
				constructor(args -> semVerType((List<VersionRange>)args.get(0)),
						arrayType(semVerRangeType())),
				constructor((ObjectType)semVerType()._pcoreType())
		);
	}

	// SensitiveType
	public static SensitiveType sensitiveType() {
		return SensitiveType.DEFAULT;
	}

	public static SensitiveType sensitiveType(AnyType type) {
		return AnyType.DEFAULT.equals(type) ? SensitiveType.DEFAULT : new SensitiveType(type);
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<SensitiveType> sensitiveTypeDispatcher() {
		return dispatcher(
				constructor(args -> sensitiveType()),
				constructor(args -> sensitiveType((AnyType)args.get(0)),
						typeType()),
				constructor((ObjectType)sensitiveType()._pcoreType())
		);
	}

	// StringType
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
		if(arg instanceof Number)
			return stringType(integerType(((Number)arg).longValue()));
		throw new IllegalArgumentException("Unable to create a String with an argument of class " + arg.getClass().getName
				());
	}

	public static StringType stringType(IntegerType size) {
		return IntegerType.POSITIVE.equals(size) ? StringType.DEFAULT : new StringType(size);
	}

	public static StringType stringType(String value) {
		return value == null ? StringType.DEFAULT : new StringType(value);
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<StringType> stringTypeDispatcher() {
		return dispatcher(
				constructor(args -> stringType()),
				constructor(args -> stringType((IntegerType)args.get(0)),
						typeType(integerType())),
				constructor(args -> stringType((String)args.get(0)),
						stringType()),
				constructor((ObjectType)stringType()._pcoreType())
		);
	}

	// StructType
	public static StructElement structElement(String key, AnyType valueType) {
		return new StructElement(key, valueType);
	}

	public static StructElement structElement(AnyType key, AnyType valueType) {
		return new StructElement(key, valueType);
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<StructElement> structElementDispatcher() {
		return dispatcher(
				constructor(args -> structElement((String)args.get(0), (AnyType)args.get(1)),
						stringType(), typeType()),
				constructor(args -> structElement((AnyType)args.get(0), (AnyType)args.get(1)),
						typeType(), typeType()),
				constructor((ObjectType)StructElement.pcoreType())
		);
	}

	public static StructType structType() {
		return StructType.DEFAULT;
	}

	public static StructType structType(StructElement... elements) {
		return elements.length == 0 ? StructType.DEFAULT : new StructType(Helpers.asList(elements));
	}

	public static StructType structType(List<StructElement> elements) {
		return elements.isEmpty() ? StructType.DEFAULT : new StructType(unmodifiableCopy(elements));
	}

	public static StructType structTypeStrings(Map<String,AnyType> elements) {
		return elements.isEmpty() ? StructType.DEFAULT : structType(map(elements.entrySet(), element -> structElement(element.getKey(), element.getValue())));
	}

	public static StructType structType(Map<AnyType,AnyType> elements) {
		return elements.isEmpty() ? StructType.DEFAULT : structType(map(elements.entrySet(), element -> structElement(element.getKey(), element.getValue())));
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<StructType> structTypeDispatcher() {
		return dispatcher(
				constructor(args -> structType()),
				constructor(args -> structType((List<StructElement>)args.get(0)),
						arrayType(StructElement.pcoreType())),
				constructor(args -> structTypeStrings((Map<String,AnyType>)args.get(0)),
						hashType(stringType(), typeType())),
				constructor(args -> structType((Map<AnyType,AnyType>)args.get(0)),
						hashType(typeType(), typeType())),
				constructor((ObjectType)structType()._pcoreType())
		);
	}

	// TargetType (this is an ObjectType), not a specialization of AnyType
	public static ObjectType targetType() {
		return ((PcoreImpl)Pcore.staticPcore()).target;
	}

	// TimeSpanType
	public static TimeSpanType timeSpanType() {
		return TimeSpanType.DEFAULT;
	}

	public static TimeSpanType timeSpanType(Duration min) {
		return timeSpanType(min, TimeSpanType.MAX_DURATION);
	}

	public static TimeSpanType timeSpanType(Duration min, Duration max) {
		return new TimeSpanType(min, max);
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<TimeSpanType> timeSpanTypeDispatcher() {
		return dispatcher(
				constructor(args -> timeSpanType()),
				constructor(args -> timeSpanType((Duration)args.get(0)),
						timeSpanType()),
				constructor(args -> timeSpanType((Duration)args.get(0), (Duration)args.get(1)),
						timeSpanType(), timeSpanType()),
				constructor((ObjectType)timeSpanType()._pcoreType())
		);
	}

	// TimestampType
	public static TimestampType timestampType() {
		return TimestampType.DEFAULT;
	}

	public static TimestampType timestampType(Instant min) {
		return timestampType(min, Instant.MAX);
	}

	public static TimestampType timestampType(Instant min, Instant max) {
		return new TimestampType(min, max);
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<TimestampType> timestampTypeDispatcher() {
		return dispatcher(
				constructor(args -> timestampType()),
				constructor(args -> timestampType((Instant)args.get(0)),
						timestampType()),
				constructor(args -> timestampType((Instant)args.get(0), (Instant)args.get(1)),
						timestampType(), timestampType()),
				constructor((ObjectType)timestampType()._pcoreType())
		);
	}

	public static TupleType tupleType() {
		return TupleType.DEFAULT;
	}

	public static TupleType tupleTypeEmpty() {
		return TupleType.EXPLICIT_EMPTY;
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

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<TupleType> tupleTypeDispatcher() {
		return dispatcher(
				constructor(args -> tupleType()),
				constructor(args -> tupleType((List<AnyType>)args.get(0)),
						arrayType(typeType())),
				constructor(args -> tupleType((List<AnyType>)args.get(0), ((Number)args.get(1)).longValue(), ((Number)args.get(2)).longValue()),
						arrayType(typeType()), integerType(), integerType()),
				constructor(args -> tupleType((List<AnyType>)args.get(0), (IntegerType)args.get(1)),
						arrayType(typeType()), optionalType(typeType(integerType()))),
				constructor((ObjectType)tupleType()._pcoreType())
		);
	}

	// TypeAliasType
	public static TypeAliasType typeAliasType() {
		return TypeAliasType.DEFAULT;
	}

	public static TypeAliasType typeAliasType(String name, Expression typeExpression) {
		return new TypeAliasType(name, typeExpression, null);
	}

	public static TypeAliasType typeAliasType(String name, TypeReferenceType typeExpression) {
		return new TypeAliasType(name, typeExpression, null);
	}

	public static TypeAliasType typeAliasType(String name, AnyType resolvedType) {
		return new TypeAliasType(name, null, resolvedType);
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<TypeAliasType> typeAliasTypeDispatcher() {
		return new SelfReferencingFactoryImpl(asList(
				constructor(args -> typeAliasType()),
				constructor(args -> typeAliasType((String)args.get(0), (AnyType)args.get(1)),
						stringType(), typeType()),
				constructor(args -> typeAliasType((String)args.get(0), (Expression)args.get(1)),
						stringType(), runtimeType(Expression.class.getName())),
				constructor((ObjectType)typeAliasType()._pcoreType())
		)) {
			@Override
			public TypeAliasType createInstance(Type type, ArgumentsAccessor aa) throws IOException {
				return new TypeAliasType(aa);
			}
		};
	}

	// TypeReferenceType
	public static TypeReferenceType typeReferenceType() {
		return TypeReferenceType.DEFAULT;
	}

	public static TypeReferenceType typeReferenceType(String typeString) {
		return new TypeReferenceType(typeString);
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<TypeReferenceType> typeReferenceTypeDispatcher() {
		return dispatcher(
				constructor(args -> typeReferenceType()),
				constructor(args -> typeReferenceType((String)args.get(0)),
						stringType()),
				constructor((ObjectType)typeReferenceType()._pcoreType())
		);
	}

	// TypeSetType
	public static TypeSetType typeSetType() {
		return TypeSetType.DEFAULT;
	}

	public static TypeSetType typeSetType(Map<String,Object> initHash) {
		return new TypeSetType(initHash);
	}

	public static TypeSetType typeSetType(String name, URI nameAuthority, Expression initExpression) {
		return new TypeSetType(name, nameAuthority, initExpression);
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<TypeSetType> typeSetTypeDispatcher() {
		return new SelfReferencingFactoryImpl(asList(
				constructor(args -> typeSetType()),
				constructor(args -> typeSetType((String)args.get(0), (URI)args.get(1), (Expression)args.get(2)),
						stringType(), runtimeType(URI.class.getName()), runtimeType(Expression.class.getName())),
				hashConstructor(args -> typeSetType((Map<String,Object>)args.get(0)), TYPE_TYPESET_INIT)
		)) {
			@Override
			public TypeSetType createInstance(Type type, ArgumentsAccessor aa) throws IOException {
				return new TypeSetType(aa);
			}
		};
	}

	// TypeType
	public static TypeType typeType() {
		return TypeType.DEFAULT;
	}

	public static TypeType typeType(AnyType type) {
		return AnyType.DEFAULT.equals(type) ? TypeType.DEFAULT : new TypeType(type);
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<TypeType> typeTypeDispatcher() {
		return dispatcher(
				constructor(args -> typeType()),
				constructor(args -> typeType((AnyType)args.get(0)),
						typeType()),
				constructor((ObjectType)typeType()._pcoreType())
		);
	}

	// UndefType
	public static UndefType undefType() {
		return UndefType.DEFAULT;
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<UndefType> undefTypeDispatcher() {
		return dispatcher(constructor(args -> undefType()));
	}

	// UnitType
	public static UnitType unitType() {
		return UnitType.DEFAULT;
	}

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<UnitType> unitTypeDispatcher() {
		return dispatcher(constructor(args -> unitType()));
	}

	// VariantType
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

	public static AnyType variantType(List<? extends AnyType> types) {
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

	@SuppressWarnings("unchecked")
	public static FactoryDispatcher<VariantType> variantTypeDispatcher() {
		FactoryDispatcher<?> fd = dispatcher(
				constructor(args -> variantType()),
				constructor(args -> variantType((List<AnyType>)args.get(0)),
						arrayType(typeType())),
				constructor((ObjectType)variantType()._pcoreType())
		);
		return (FactoryDispatcher<VariantType>)fd;
	}

	public static AnyType[] registerPcoreTypes(PcoreImpl pcore) {
		return new AnyType[] {
			AnyType.registerPcoreType(pcore),
			ArrayType.registerPcoreType(pcore),
			BinaryType.registerPcoreType(pcore),
			BooleanType.registerPcoreType(pcore),
			CallableType.registerPcoreType(pcore),
			CatalogEntryType.registerPcoreType(pcore),
			ClassType.registerPcoreType(pcore),
			CollectionType.registerPcoreType(pcore),
			DefaultType.registerPcoreType(pcore),
			EnumType.registerPcoreType(pcore),
			FloatType.registerPcoreType(pcore),
			HashType.registerPcoreType(pcore),
			InitType.registerPcoreType(pcore),
			IntegerType.registerPcoreType(pcore),
			IterableType.registerPcoreType(pcore),
			IteratorType.registerPcoreType(pcore),
			NotUndefType.registerPcoreType(pcore),
			NumericType.registerPcoreType(pcore),
			ObjectType.registerPcoreType(pcore),
			OptionalType.registerPcoreType(pcore),
			PatternType.registerPcoreType(pcore),
			RegexpType.registerPcoreType(pcore),
			ResourceType.registerPcoreType(pcore),
			RuntimeType.registerPcoreType(pcore),
			ScalarType.registerPcoreType(pcore),
			ScalarDataType.registerPcoreType(pcore),
			SemVerType.registerPcoreType(pcore),
			SemVerRangeType.registerPcoreType(pcore),
			SensitiveType.registerPcoreType(pcore),
			StringType.registerPcoreType(pcore),
			StructElement.registerPcoreType(pcore),
			StructType.registerPcoreType(pcore),
			TimeSpanType.registerPcoreType(pcore),
			TimestampType.registerPcoreType(pcore),
			TupleType.registerPcoreType(pcore),
			TypeAliasType.registerPcoreType(pcore),
			TypeReferenceType.registerPcoreType(pcore),
			TypeSetType.registerPcoreType(pcore),
			TypeType.registerPcoreType(pcore),
			UndefType.registerPcoreType(pcore),
			UnitType.registerPcoreType(pcore),
			VariantType.registerPcoreType(pcore),
		};
	}

	public static void registerImpls(PcoreImpl pcore) {
				AnyType.registerImpl(pcore);
				ArrayType.registerImpl(pcore);
				BinaryType.registerImpl(pcore);
				BooleanType.registerImpl(pcore);
				CallableType.registerImpl(pcore);
				CatalogEntryType.registerImpl(pcore);
				ClassType.registerImpl(pcore);
				CollectionType.registerImpl(pcore);
				DefaultType.registerImpl(pcore);
				EnumType.registerImpl(pcore);
				FloatType.registerImpl(pcore);
				HashType.registerImpl(pcore);
				InitType.registerImpl(pcore);
				IntegerType.registerImpl(pcore);
				IterableType.registerImpl(pcore);
				IteratorType.registerImpl(pcore);
				NotUndefType.registerImpl(pcore);
				NumericType.registerImpl(pcore);
				ObjectType.registerImpl(pcore);
				OptionalType.registerImpl(pcore);
				PatternType.registerImpl(pcore);
				RegexpType.registerImpl(pcore);
				ResourceType.registerImpl(pcore);
				RuntimeType.registerImpl(pcore);
				ScalarType.registerImpl(pcore);
				ScalarDataType.registerImpl(pcore);
				SemVerType.registerImpl(pcore);
				SemVerRangeType.registerImpl(pcore);
				SensitiveType.registerImpl(pcore);
				StringType.registerImpl(pcore);
				StructElement.registerImpl(pcore);
				StructType.registerImpl(pcore);
				TimeSpanType.registerImpl(pcore);
				TimestampType.registerImpl(pcore);
				TupleType.registerImpl(pcore);
				TypeAliasType.registerImpl(pcore);
				TypeReferenceType.registerImpl(pcore);
				TypeSetType.registerImpl(pcore);
				TypeType.registerImpl(pcore);
				UndefType.registerImpl(pcore);
				UnitType.registerImpl(pcore);
				VariantType.registerImpl(pcore);
	}
}
