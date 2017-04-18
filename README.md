# PropertyFile plug-in for [Kobalt](http://beust.com/kobalt/home/index.html)

[![License (3-Clause BSD)](https://img.shields.io/badge/license-BSD%203--Clause-blue.svg?style=flat-square)](http://opensource.org/licenses/BSD-3-Clause) [![Build Status](https://travis-ci.org/ethauvin/kobalt-property-file.svg?branch=master)](https://travis-ci.org/ethauvin/kobalt-property-file) 

The PropertyFile plug-in provides an optional task for editing [property files](https://docs.oracle.com/javase/tutorial/essential/environment/properties.html). It is inspired by the [ant PropertyFile task](https://ant.apache.org/manual/Tasks/propertyfile.html).

```kotlin
import net.thauvin.erik.kobalt.plugin.propertyfile.*

val bs = buildScript {
    plugins("net.thauvin.erik:kobalt-property-file:")
}

val p = project {
    name = "example"

    propertyFile {
       file = "version.properties"
       comment = "##Generated file - do not modify!"
       entry(key = "product.build.major", type = Types.INT, value = "3")
       entry(key = "product.build.minor", type = Types.INT, operation = Operations.ADD)
       entry(key = "product.build.patch", type = Types.INT, value = "0")
       entry(key = "product.build.date" , type = Types.DATE, value = "now")
    }
}
```
[View Example](https://github.com/ethauvin/kobalt-property-file/blob/master/example/kobalt/src/Build.kt)

To invoke the `propertyFile` task:

```sh
./kobaltw propertyFile
```

## Parameters

Attribute | Description                                             | Required
:---------|:--------------------------------------------------------|:--------
`file`    | The location of the property files to edit.             | Yes
`comment` | Comment to be inserted at the top of the property file. | No

## Entry

The `entry` function is used to specify modifications to be made to the property file.

Attribute   | Description                                                                                                       | Required
:-----------|:----------------------------------------------------------------------------------------------------------------- |:----------------------------------------------
`key`       | The name of the property name/value pair.                                                                         | Yes, unless `operation` is `Operations.DELETE`
`value`     | The value of the property.                                                                                        | Yes, unless `operation` is `Operations.DELETE`
`default`   | The initial value to set for the property if not already defined. For `Type.DATE`, the `now` keyword can be used. | Yes, unless `operation` is `Operations.DELETE`
`type`      | Tread the value as `Types.INT`, `Types.DATE`, or `Types.STRING` (default).                                        | No
`operation` | See [operations](#operations).                                                                                     | No
`pattern`   | For `Types.INT` and `Types.DATE` only. If present, will parse the value as [DecimalFormat](https://docs.oracle.com/javase/7/docs/api/java/text/DecimalFormat.html) or [SimpleDateFormat](https://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html) pattern, respectively. | No
`unit`      | The unit value to be applied to `Operations.ADD` and `Operations.SUBTRACT` for `Types.DATE`. See [Units](#units). | No

## Operations

The following operations are available:

Operation             | Description
:---------------------|:-------------------------------------------------------------------------
`Operations.ADD`      | Adds a value to an entry.
`Operations.DELETE`   | Deletes an entry.
`Operations.SET`      | Sets the entry value (default).
`Operations.SUBTRACT` | Subtracts a value from the entry. For `Types.INT` and `Types.DATE` only.

## Units

The following units are available for `Types.DATE` with `Operations.ADD` and `Operations.SUBTRACT`:

* Units.MILLISECOND
* Units.SECOND
* Units.MINUTE
* Units.HOUR
* Units.DAY
* Units.WEEK
* Units.MONTH
* Units.YEAR

## Rules

The rules used when setting a property value are:

* If only `value` is specified, the property is set to it regardless of its previous value.
* If only `default` is specified and the property previously existed, it is unchanged.
* If only `default` is specified and the property did not exist, the property is set to `default`.
* If `value` and `default` are both specified and the property previously existed, the property is set to `value`.
* If `value` and `default` are both specified and the property did not exist, the property is set to `default`.

Operations occur after the rules are evaluated.

## Differences with the [ant PropertyFile task](https://ant.apache.org/manual/Tasks/propertyfile.html)

* The comments and layout of the original property file will not be preserved.
* The default `Types.DATE` pattern is `yyyy-MM-dd HH:mm` and not `yyyy/MM/dd HH:mm`.