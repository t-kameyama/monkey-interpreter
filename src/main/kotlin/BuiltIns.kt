val builtIns = mapOf(
        "len" to Builtin {
            val arg = it.singleOrNull() ?: return@Builtin Error("wrong number of arguments. got=${it.size}, want=1")
            when (arg) {
                is Str -> Integer(arg.value.length)
                is Arr -> Integer(arg.elements.size)
                else -> Error("argument to `len` not supported, got ${arg.type}")
            }
        },
        "first" to Builtin {
            val arg = it.singleOrNull() ?: return@Builtin Error("wrong number of arguments. got=${it.size}, want=1")
            when (arg) {
                is Arr -> arg.elements.firstOrNull() ?: Null
                else -> Error("argument to `first` not supported, got ${arg.type}")
            }
        },
        "rest" to Builtin {
            val arg = it.singleOrNull() ?: return@Builtin Error("wrong number of arguments. got=${it.size}, want=1")
            when (arg) {
                is Arr -> Arr(arg.elements.drop(1))
                else -> Error("argument to `rest` not supported, got ${arg.type}")
            }
        },
        "last" to Builtin {
            val arg = it.singleOrNull() ?: return@Builtin Error("wrong number of arguments. got=${it.size}, want=1")
            when (arg) {
                is Arr -> arg.elements.lastOrNull() ?: Null
                else -> Error("argument to `last` not supported, got ${arg.type}")
            }
        },
        "push" to Builtin {
            if (it.size != 2) return@Builtin Error("wrong number of arguments. got=${it.size}, want=1")
            val first = it[0]
            val second = it[1]
            when {
                first is Arr && second is Integer -> Arr(first.elements + listOf(second))
                else -> Error("argument to `push` not supported, got ${first.type}, ${second.type}")
            }
        },
        "puts" to Builtin { args ->
            args.forEach { println(it.inspect) }
            Null
        }
)