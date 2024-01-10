package com.kcvn.spm.common.batch.excel.mapping

import com.kcvn.spm.common.batch.excel.RowMapper
import com.kcvn.spm.common.batch.excel.rowset.RowSet
import org.springframework.batch.support.DefaultPropertyEditorRegistrar
import org.springframework.beans.*
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.InitializingBean
import org.springframework.util.Assert
import org.springframework.validation.BindException
import org.springframework.validation.DataBinder
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * [RowMapper] implementation based on bean property paths. The [RowSet] to be
 * mapped should have field name metadata corresponding to bean property paths in an
 * instance of the desired type. The instance is created and initialized either by
 * referring to a prototype object by bean name in the enclosing BeanFactory, or by
 * providing a class to instantiate reflectively.<br></br>
 * <br></br>
 *
 * Nested property paths, including indexed properties in maps and collections, can be
 * referenced by the [RowSet]names. They will be converted to nested bean properties
 * inside the prototype. The [RowSet] and the prototype are thus tightly coupled by
 * the fields that are available and those that can be initialized. If some of the nested
 * properties are optional (e.g. collection members) they need to be removed by a post
 * processor.<br></br>
 * <br></br>
 *
 * To customize the way that [RowSet] values are converted to the desired type for
 * injecting into the prototype there are several choices. You can inject
 * [java.beans.PropertyEditor] instances directly through the
 * [customEditors][.setCustomEditors] property, or you can override the
 * [.createBinder] and [.initBinder] methods, or you can
 * provide a custom [RowSet] implementation.<br></br>
 * <br></br>
 *
 * Property name matching is "fuzzy" in the sense that it tolerates close matches, as long
 * as the match is unique. For instance:
 *
 *
 *  * Quantity = quantity (field names can be capitalised)
 *  * ISIN = isin (acronyms can be lowered case bean property names, as per Java Beans
 * recommendations)
 *  * DuckPate = duckPate (capitalisation including camel casing)
 *  * ITEM_ID = itemId (capitalisation and replacing word boundary with underscore)
 *  * ORDER.CUSTOMER_ID = order.customerId (nested paths are recursively checked)
 *
 *
 * The algorithm used to match a property name is to start with an exact match and then
 * search successively through more distant matches until precisely one match is found. If
 * more than one match is found there will be an error.
 *
 * @param <T> type
 */
class BeanWrapperRowMapper<T> : DefaultPropertyEditorRegistrar(),
    RowMapper<T>, BeanFactoryAware, InitializingBean {
    private var name: String? = null
    private var type: Class<out T>? = null
    private var beanFactory: BeanFactory? = null
    private val propertiesMatched: ConcurrentMap<DistanceHolder, ConcurrentMap<String?, String>> = ConcurrentHashMap()
    private var distanceLimit = 5
    private var strict = true
    override fun setBeanFactory(beanFactory: BeanFactory) {
        this.beanFactory = beanFactory
    }

    /**
     * The maximum difference that can be tolerated in spelling between input key names
     * and bean property names. Defaults to 5, but could be set lower if the field names
     * match the bean names.
     * @param distanceLimit the distance limit to set
     */
    fun setDistanceLimit(distanceLimit: Int) {
        this.distanceLimit = distanceLimit
    }

    /**
     * The bean name (id) for an object that can be populated from the field set that will
     * be passed into [.mapRow]. Typically, a prototype scoped bean so that a
     * new instance is returned for each field set mapped.
     * Either this property or the type property must be specified, but not both.
     * @param name the name of a prototype bean in the enclosing BeanFactory
     */
    fun setPrototypeBeanName(name: String?) {
        this.name = name
    }

    /**
     * Public setter for the type of bean to create instead of using a prototype bean. An
     * object of this type will be created from its default constructor for every call to
     * [.mapRow].<br></br>
     *
     * Either this property or the prototype bean name must be specified, but not both.
     * @param type the type to set
     */
    fun setTargetType(type: Class<out T>?) {
        this.type = type
    }

    /**
     * Check that precisely one of type or prototype bean name is specified.
     * @throws IllegalStateException if neither is set nor both properties are set.
     * @see org.springframework.beans.factory.InitializingBean.afterPropertiesSet
     */
    override fun afterPropertiesSet() {
        Assert.state(name != null || type != null, "Either name or type must be provided.")
        Assert.state(name == null || type == null, "Both name and type cannot be specified together.")
    }

    /**
     * Map the [org.springframework.batch.item.file.transform.FieldSet] to an object
     * retrieved from the enclosing Spring context, or to a new instance of the required
     * type if no prototype is available.
     * @throws org.springframework.validation.BindException if there is a type conversion
     * or other error (if the [org.springframework.validation.DataBinder] from
     * [.createBinder] has errors after binding).
     * @throws org.springframework.beans.NotWritablePropertyException if the
     * [org.springframework.batch.item.file.transform.FieldSet] contains a field
     * that cannot be mapped to a bean property.
     * @see org.springframework.batch.item.file.mapping.FieldSetMapper.mapFieldSet
     */
    @Throws(BindException::class)
    override fun mapRow(rs: RowSet): T {
        val copy = bean
        val binder = createBinder(copy)
        binder.bind(MutablePropertyValues(getBeanProperties(copy as Any, rs.getProperties()!!)))
        if (binder.bindingResult.hasErrors()) {
            throw BindException(binder.bindingResult)
        }
        return copy
    }

    /**
     * Create a binder for the target object. The binder will then be used to bind the
     * properties form a field set into the target object. This implementation creates a
     * new [DataBinder] and calls out to [.initBinder] and
     * [.registerCustomEditors].
     * @param target the object to bind to.
     * @return a [DataBinder] that can be used to bind properties to the target.
     */
    protected fun createBinder(target: Any?): DataBinder {
        val binder = DataBinder(target)
        binder.isIgnoreUnknownFields = !strict
        initBinder(binder)
        registerCustomEditors(binder)
        return binder
    }

    /**
     * Initialize a new binder instance. This hook allows customization of binder settings
     * such as the [direct field access][DataBinder.initDirectFieldAccess]. Called
     * by [.createBinder].
     *
     *
     * Note that registration of custom property editors can be done in
     * [.registerCustomEditors].
     *
     * @param binder new binder instance
     * @see .createBinder
     */
    protected fun initBinder(binder: DataBinder?) {

    }

    private val bean: T
        get() = if (name != null) {
            beanFactory!!.getBean(name!!) as T
        } else BeanUtils.instantiateClass(type!!)

    private fun getBeanProperties(bean: Any, properties: Properties): Properties {
        val cls: Class<*> = bean.javaClass

        // Map from field names to property names
        val distanceKey = DistanceHolder(cls, distanceLimit)
        if (!propertiesMatched.containsKey(distanceKey)) {
            propertiesMatched.putIfAbsent(distanceKey, ConcurrentHashMap())
        }
        val matches: MutableMap<String?, String> = HashMap(
            propertiesMatched[distanceKey]
        )
        val keys: Set<String?> = HashSet(properties.keys.map { it.toString() })
        for (key in keys) {
            if (matches.containsKey(key)) {
                switchPropertyNames(properties, key, matches[key])
                continue
            }
            val name = findPropertyName(bean, key)
            if (name != null) {
                if (matches.containsValue(name)) {
                    throw NotWritablePropertyException(
                        cls, name, "Duplicate match with distance <= "
                                + distanceLimit + " found for this property in input keys: " + keys
                                + ". (Consider reducing the distance limit or changing the input key names to get a closer match.)"
                    )
                }
                matches[key] = name
                switchPropertyNames(properties, key, name)
            }
        }
        propertiesMatched.replace(distanceKey, ConcurrentHashMap(matches))
        return properties
    }

    private fun findPropertyName(bean: Any?, key: String?): String? {
        if (bean == null) {
            return null
        }
        val cls: Class<*> = bean.javaClass
        var index = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(key!!)
        val prefix: String?
        val suffix: String

        // If the property name is nested recurse down through the properties
        // looking for a match.
        if (index > 0) {
            prefix = key.substring(0, index)
            suffix = key.substring(index + 1)
            val nestedName = findPropertyName(bean, prefix) ?: return null
            val nestedValue = getPropertyValue(bean, nestedName)
            val nestedPropertyName = findPropertyName(nestedValue, suffix)
            return if (nestedPropertyName != null) "$nestedName.$nestedPropertyName" else null
        }
        var name: String? = null
        var distance = 0
        index = key.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR)
        if (index > 0) {
            prefix = key.substring(0, index)
            suffix = key.substring(index)
        } else {
            prefix = key
            suffix = ""
        }
        while (name == null && distance <= distanceLimit) {
            val candidates = PropertyMatches.forProperty(prefix, cls, distance).possibleMatches
            // If we find precisely one match, then use that one...
            if (candidates.size == 1) {
                val candidate = candidates[0]
                name = if (candidate == prefix) { // if it's the same don't
                    // replace it...
                    key
                } else {
                    candidate + suffix
                }
            }
            distance++
        }
        return name
    }

    private fun getPropertyValue(bean: Any, nestedName: String): Any? {
        val wrapper = BeanWrapperImpl(bean)
        wrapper.isAutoGrowNestedPaths = true
        var nestedValue = wrapper.getPropertyValue(nestedName)
        if (nestedValue == null) {
            nestedValue = BeanUtils.instantiateClass(wrapper.getPropertyType(nestedName)!!)
            wrapper.setPropertyValue(nestedName, nestedValue)
        }
        return nestedValue
    }

    private fun switchPropertyNames(properties: Properties, oldName: String?, newName: String?) {
        val value = properties.getProperty(oldName)
        properties.remove(oldName)
        properties.setProperty(newName, value)
    }

    /**
     * Public setter for the 'strict' property. If true, then [.mapRow] will
     * fail if the RowSet contains fields that cannot be mapped to the bean.
     * @param strict fail if non-mappable properties are found
     */
    fun setStrict(strict: Boolean) {
        this.strict = strict
    }

    private class DistanceHolder internal constructor(private val cls: Class<*>?, private val distance: Int) {
        override fun equals(obj: Any?): Boolean {
            if (this === obj) {
                return true
            }
            if (obj == null) {
                return false
            }
            if (javaClass != obj.javaClass) {
                return false
            }
            val other = obj as DistanceHolder
            if (cls == null) {
                if (other.cls != null) {
                    return false
                }
            } else if (cls != other.cls) {
                return false
            }
            return distance == other.distance
        }

        override fun hashCode(): Int {
            val prime = 31
            var result = 1
            result = prime * result + (cls?.hashCode() ?: 0)
            result = prime * result + distance
            return result
        }
    }
}
