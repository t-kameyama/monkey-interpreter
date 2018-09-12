enum class ObjectType {
    INTEGER,
    BOOLEAN,
    STRING,
    FUNCTION,
    ARRAY,
    HASH,
    RETURN,
    NULL,
    ERROR,
    BUILTIN
}

interface Hashable

interface Object {
    val type: ObjectType
    val inspect: String
}

data class Integer(val value: Int) : Object, Hashable {
    override val type
        get() = ObjectType.INTEGER
    override val inspect
        get() = "$value"
}

data class Bool(val value: Boolean) : Object, Hashable {
    override val type
        get() = ObjectType.BOOLEAN
    override val inspect
        get() = "$value"
}

data class Str(val value: String) : Object, Hashable {
    override val type
        get() = ObjectType.STRING
    override val inspect
        get() = value
}

data class Function(val parameters: List<IdentifierExpression>, val body: BlockStatement, val env: Environment): Object {
    override val type
        get() = ObjectType.FUNCTION
    override val inspect
        get() = buildString {
            append("fn(")
            append(parameters.joinToString(", ") { it.text })
            append(") { ")
            append(body.text)
            append(" }")
        }
}

data class Arr(val elements: List<Object>): Object {
    override val type
        get() = ObjectType.ARRAY
    override val inspect
        get() = buildString {
            append("[")
            append(elements.joinToString(separator = ", ") { it.inspect })
            append("]")
        }
}

data class Hash(val pairs: Map<Object, Object>): Object {
    override val type
        get() = ObjectType.HASH
    override val inspect
        get() = buildString {
            append("[")
            append(pairs.toList().joinToString(separator = ", ") { "$" })
            append("]")
        }
}

data class Return(val value: Object) : Object {
    override val type
        get() = ObjectType.RETURN
    override val inspect
        get() = value.inspect
}

object Null : Object {
    override val type
        get() = ObjectType.NULL
    override val inspect
        get() = "null"
}

data class Error(val message: String) : Object {
    override val type
        get() = ObjectType.ERROR
    override val inspect
        get() = "ERROR: $message"
}

data class Builtin(val fn: (List<Object>) -> Object) : Object {
    override val type
        get() = ObjectType.BUILTIN
    override val inspect
        get() = "builtin function"
}
