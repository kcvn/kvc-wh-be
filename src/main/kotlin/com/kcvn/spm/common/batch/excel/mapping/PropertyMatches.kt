package com.kcvn.spm.common.batch.excel.mapping

import org.springframework.beans.BeanUtils
import org.springframework.util.StringUtils
import java.beans.PropertyDescriptor
import java.util.*
import kotlin.math.min

/**
 * Helper class for calculating bean property matches, according to. Used by
 * BeanWrapperImpl to suggest alternatives for an invalid property name.<br></br>
 *
 * Copied and slightly modified from Spring core,
 *
 * @see #forProperty(String, Class, int)
 */
internal class PropertyMatches
/**
 * Create a new PropertyMatches instance for the given property.
 * @param propertyName the name of the property to find possible matches for
 * @param beanClass the bean class to search for matches
 * @param maxDistance the maximum property distance allowed for matches
 */
private constructor(
    private val propertyName: String,
    beanClass: Class<*>,
    maxDistance: Int
) {
    val possibleMatches: Array<String>

    init {
        possibleMatches = calculateMatches(BeanUtils.getPropertyDescriptors(beanClass), maxDistance)
    }

    /**
     * Generate possible property alternatives for the given property and class.
     * Internally uses the `getStringDistance` method, which in turn uses the
     * Levenshtein algorithm to determine the distance between two Strings.
     * @param propertyDescriptors the JavaBeans property descriptors to search
     * @param maxDistance the maximum distance to accept
     * @return the calculated matches
     */
    private fun calculateMatches(propertyDescriptors: Array<PropertyDescriptor>, maxDistance: Int): Array<String> {
        val candidates: MutableList<String> = ArrayList()
        for (propertyDescriptor in propertyDescriptors) {
            if (propertyDescriptor.getWriteMethod() != null) {
                val possibleAlternative = propertyDescriptor.name
                val distance = calculateStringDistance(propertyName, possibleAlternative)
                if (distance <= maxDistance) {
                    candidates.add(possibleAlternative)
                }
            }
        }
        candidates.sort()
        return StringUtils.toStringArray(candidates)
    }

    /**
     * Calculate the distance between the given two Strings according to the Levenshtein
     * algorithm.
     * @param s1 the first String
     * @param s2 the second String
     * @return the distance value
     */
    private fun calculateStringDistance(s1: String, s2: String): Int {
        if (s1.isEmpty()) {
            return s2.length
        }
        if (s2.isEmpty()) {
            return s1.length
        }
        val d = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) {
            d[i][0] = i
        }
        for (j in 0..s2.length) {
            d[0][j] = j
        }
        for (i in 1..s1.length) {
            val s_i = s1[i - 1]
            for (j in 1..s2.length) {
                var cost: Int
                val t_j = s2[j - 1]
                cost = if (s_i.lowercaseChar() == t_j.lowercaseChar()) {
                    0
                } else {
                    1
                }
                d[i][j] = min(
                    min((d[i - 1][j] + 1).toDouble(), (d[i][j - 1] + 1).toDouble()),
                    (d[i - 1][j - 1] + cost).toDouble()
                )
                    .toInt()
            }
        }
        return d[s1.length][s2.length]
    }

    companion object {
        /**
         * Create PropertyMatches for the given bean property.
         * @param propertyName the name of the property to find possible matches for
         * @param beanClass the bean class to search for matches
         * @param maxDistance the maximum property distance allowed for matches
         * @return the prepared `PropertyMatches`
         */
        fun forProperty(propertyName: String, beanClass: Class<*>, maxDistance: Int): PropertyMatches {
            return PropertyMatches(propertyName, beanClass, maxDistance)
        }
    }
}
