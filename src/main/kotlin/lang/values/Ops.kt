package org.example.lang.values


operator fun Val.plus(other : Val) : AValue<Val> = when(this)
{
    is Val.IntV -> when(other){
        is Val.IntV -> AValue.Const(Val.IntV(this.v + other.v))
        else -> AValue.Unknown

    }
    else -> AValue.Unknown
}

operator fun Val.minus(other : Val) : AValue<Val> = when(this)
{
    is Val.IntV -> when(other)
    {
        is Val.IntV -> AValue.Const(Val.IntV(this.v - other.v))
        else -> AValue.Unknown
    }
    else -> AValue.Unknown
}

operator fun Val.times(other : Val) : AValue<Val> = when(this)
{
    is Val.IntV -> when(other)
    {
        is Val.IntV -> AValue.Const(Val.IntV(this.v * other.v))
        else -> AValue.Unknown
    }
    else -> AValue.Unknown
}

fun Val.Equals(other : Val) : AValue<Val> = when(this)
{
    is Val.BoolV -> when(other)
    {
        is Val.BoolV -> AValue.Const(Val.BoolV(this.v == other.v))
        else -> AValue.Unknown
    }
    is Val.IntV -> when(other)
    {
        is Val.IntV -> AValue.Const(Val.BoolV(this.v == other.v))
        else -> AValue.Unknown
    }
    else -> AValue.Unknown
}

fun Val.NotEquals(other : Val) : AValue<Val> = when(this)
{
    is Val.BoolV -> when(other)
    {
        is Val.BoolV -> AValue.Const(Val.BoolV(this.v != other.v))
        else -> AValue.Unknown
    }
    is Val.IntV -> when(other)
    {
        is Val.IntV -> AValue.Const(Val.BoolV(this.v != other.v))
        else -> AValue.Unknown
    }
    else -> AValue.Unknown
}

fun Val.Lt(other : Val) : AValue<Val> = when(this)
{
    is Val.IntV -> when(other)
    {
        is Val.IntV -> AValue.Const(Val.BoolV(this.v < other.v))
        else -> AValue.Unknown
    }
    else -> AValue.Unknown
}