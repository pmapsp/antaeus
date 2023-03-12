package io.pleo.antaeus.core.exceptions

class IntervalBetweenTriesInSecException : Exception("Invalid interval between tries. " +
    "The number has to be bigger or equal to 0")
