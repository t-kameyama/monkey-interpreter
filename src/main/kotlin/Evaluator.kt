fun eval(node: Node, env: Environment): Object {
    return when (node) {
        is Program -> evalProgram(node, env)
        is BlockStatement -> evalBlockStatement(node, env)
        is ExpressionStatement -> eval(node.expression, env)
        is ReturnStatement -> {
            val value = eval(node.value, env)
            if (value is Error) return value
            Return(value)
        }
        is LetStatement -> {
            val value = eval(node.value, env)
            if (value is Error) return value
            env.set(node.name.value, value)
        }
        is FunctionExpression -> {
            Function(node.parameters, node.body, env)
        }
        is CallExpression -> {
            val function = eval(node.function, env)
            if (function is Error) return function
            val args = evalExpressions(node.arguments, env)
            if (args.singleOrNull() is Error) return args.first()
            applyFunction(function, args)
        }
        is IndexExpression -> {
            val left = eval(node.left, env)
            if (left is Error) return left
            val index = eval(node.index, env)
            if (index is Error) return index
            return evalIndexExpression(left, index)
        }
        is ArrayExpression -> {
            val elements = evalExpressions(node.elements, env)
            if (elements.singleOrNull() is Error) return elements.first()
            Arr(elements)
        }
        is HashExpression -> evalHashExpression(node, env)
        is PrefixExpression -> {
            val right = eval(node.right, env)
            if (right is Error) return right
            evalPrefixExpression(node.operator, right)
        }
        is BinaryExpression -> {
            val left = eval(node.left, env)
            if (left is Error) return left
            val right = eval(node.right, env)
            if (right is Error) return right
            evalBinaryExpression(node.operator, left, right)
        }
        is IfExpression -> evalIfExpression(node, env)
        is IntegerExpression -> Integer(node.value)
        is BoolExpression -> if (node.value) TRUE else FALSE
        is StringExpression -> Str(node.value)
        is IdentifierExpression -> evalIdentifierExpression(node, env)
        else ->
            Null
    }
}

private fun evalProgram(program: Program, env: Environment): Object {
    return program.statements.fold<Statement, Object>(Null) { _, stmt ->
        val o = eval(stmt, env)
        when (o) {
            is Return -> return o.value
            is Error -> return o
        }
        o
    }
}

private fun evalBlockStatement(block: BlockStatement, env: Environment): Object {
    return block.statements.fold<Statement, Object>(Null) { _, stmt ->
        val o = eval(stmt, env)
        when (o) {
            is Return -> return o
            is Error -> return o
        }
        o
    }
}

private fun evalExpressions(expressions: List<Expression>, env: Environment): List<Object> {
    return expressions.map {
        val o = eval(it, env)
        if (o is Error) return listOf(o)
        o
    }
}

private fun applyFunction(fn: Object, args: List<Object>): Object {
    return when (fn) {
        is Function -> unwrapReturnValue(
                eval(fn.body, extendFunctionEnv(fn, args))
        )
        is Builtin -> fn.fn.invoke(args)
        else -> Error("not a function: ${fn.type}")
    }
}

private fun extendFunctionEnv(fn: Function, args: List<Object>): Environment {
    val env = Environment(fn.env)
    fn.parameters.forEachIndexed { index, param ->
        val arg = args.getOrNull(index) ?: return@forEachIndexed
        env.set(param.value, arg)
    }
    return env
}

private fun unwrapReturnValue(obj: Object): Object {
    return (obj as? Return)?.value ?: obj
}

private fun evalIndexExpression(left: Object, index: Object): Object {
    return when {
        left is Arr && index is Integer -> evalArrayIndexExpression(left, index)
        left is Hash -> evalHashIndexExpression(left, index)
        else -> Error("index operator not supported: ${left.type}")
    }
}

private fun evalArrayIndexExpression(arr: Arr, index: Integer): Object {
    val max = arr.elements.size - 1
    if (index.value < 0 || index.value > max) return Null
    return arr.elements[index.value]
}

private fun evalHashIndexExpression(hash: Hash, index: Object): Object {
    if (index !is Hashable) return Error("unusable as hash key: ${index.type}")
    return hash.pairs[index] ?: Null
}

private fun evalHashExpression(hash: HashExpression, env: Environment): Object {
    val pairs = hash.pairs.associate { (keyExpression, valueExpression) ->
        val key = eval(keyExpression, env).also {
            if (it is Error) return it
            if (it !is Hashable) return Error("unusable as hash key: ${it.type}")
        }
        val value = eval(valueExpression, env)
        if (value is Error) return value
        key to value
    }
    return Hash(pairs)
}

private fun evalPrefixExpression(operator: String, right: Object): Object {
    return when (operator) {
        "!" -> evalBangOperatorExpression(right)
        "-" -> evalMinusPrefixOperatorExpression(right)
        else -> Error("unknown operator: $operator ${right.type}")
    }
}

private fun evalBinaryExpression(operator: String, left: Object, right: Object): Object {
    return when {
        left is Integer && right is Integer -> evalIntegerBinaryExpression(operator, left, right)
        left is Str && right is Str -> evalStrBinaryExpression(operator, left, right)
        operator == "==" -> boolObject(left == right)
        operator == "!=" -> boolObject(left != right)
        left.type != right.type -> Error("type mismatch: ${left.type} $operator ${right.type}")
        else -> Error("unknown operator: ${left.type} $operator ${right.type}")
    }
}

private fun evalIfExpression(ie: IfExpression, env: Environment): Object {
    val condition = eval(ie.condition, env)
    if (condition is Error) return condition
    if (isTruthy(condition)) return eval(ie.consequence, env)
    return ie.alternative?.let { eval(it, env) } ?: Null
}


private fun isTruthy(obj: Object): Boolean {
    return when (obj) {
        is Null, FALSE -> false
        else -> true
    }
}

private fun evalIntegerBinaryExpression(operator: String, left: Integer, right: Integer): Object {
    return when (operator) {
        "+" -> Integer(left.value + right.value)
        "-" -> Integer(left.value - right.value)
        "*" -> Integer(left.value * right.value)
        "/" -> Integer(left.value / right.value)
        "<" -> boolObject(left.value < right.value)
        ">" -> boolObject(left.value > right.value)
        "==" -> boolObject(left.value == right.value)
        "!=" -> boolObject(left.value != right.value)
        else -> Error("unknown operator: ${left.type} $operator ${right.type}")
    }
}

private fun evalStrBinaryExpression(operator: String, left: Str, right: Str): Object {
    return when (operator) {
        "+" -> Str(left.value + right.value)
        else -> Error("unknown operator: ${left.type} $operator ${right.type}")
    }
}

private fun evalBangOperatorExpression(right: Object): Object {
    return when (right) {
        TRUE -> FALSE
        FALSE -> TRUE
        Null -> TRUE
        else -> FALSE
    }
}

private fun evalMinusPrefixOperatorExpression(right: Object): Object {
    return if (right is Integer) Integer(-right.value) else Error("unknown operator: -${right.type}")
}

private fun evalIdentifierExpression(node: IdentifierExpression, env: Environment): Object {
    return env.get(node.value)
            ?: builtIns[node.value]
            ?: Error("identifier not found: ${node.value}")
}

private val TRUE = Bool(true)
private val FALSE = Bool(false)
private fun boolObject(value: Boolean) = if (value) TRUE else FALSE