class Environment(private val outer: Environment? = null) {

    private val map: MutableMap<String, Object> = mutableMapOf()

    fun get(name: String): Object? {
        return map[name] ?: outer?.get(name)
    }

    fun set(name: String, value: Object): Object {
        map[name] = value
        return value
    }
}