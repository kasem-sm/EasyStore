/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor.generator

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import kasem.sm.easystore.processor.StoreVisitor

internal class StoreFileGenerator(
    visitor: StoreVisitor
) {
    private val packageName: String = visitor.packageName
    private val className: String = visitor.className
    private val generatedImports: List<String> = visitor.generatedImportNames
    private val generatedFunctions: List<FunSpec> = visitor.generatedFunctions
    private val generatedProperties: List<PropertySpec> = visitor.generatedProperties

    val fileSpec = buildFile(
        packageName = packageName,
        className = className
    )

    init {
        fileSpec.apply {
            addFileComment("This class is generated by EasyStore (https://github.com/kasem-sm/EasyStore)")

            addNecessaryDataStoreImports()
            // Other optional imports
            generatedImports.forEach {
                addImport(packageName = "androidx.datastore.preferences.core", it)
            }

            addType(
                TypeSpec.classBuilder(className).apply {
                    // Class constructor
                    buildAndAddPropertiesToClassConstructor()
                    // Generated functions
                    generatedFunctions.forEach {
                        addFunction(it)
                    }
                    // Companion Objects with generated datastore properties
                    addType(
                        TypeSpec.companionObjectBuilder().apply {
                            addModifiers(KModifier.PRIVATE)
                            generatedProperties.forEach {
                                addProperty(it)
                            }
                        }.build()
                    )
                }.build()
            )
        }
    }

    private fun TypeSpec.Builder.buildAndAddPropertiesToClassConstructor() {
        val type = DataStore::class.asClassName()
            .parameterizedBy(Preferences::class.asClassName())
        primaryConstructor(
            FunSpec.constructorBuilder().apply {
                addParameter(
                    name = "dataStore",
                    type = type
                )
            }.build()
        )
        addProperty(
            PropertySpec.builder(
                name = "dataStore",
                type = type
            ).apply {
                initializer("dataStore")
                addModifiers(KModifier.PRIVATE).build()
            }.build()
        )
    }

    private fun FileSpec.Builder.addNecessaryDataStoreImports() {
        addImport("kotlinx.coroutines.flow", "Flow", "flow", "catch", "map")
        addImport(
            "androidx.datastore.preferences.core",
            "emptyPreferences",
            "Preferences",
            "edit"
        )
        addImport("java.io", "IOException")
        addImport("androidx.datastore.core", "DataStore")
    }

    private fun buildFile(
        packageName: String,
        className: String
    ): FileSpec.Builder {
        return FileSpec.builder(
            packageName = packageName,
            fileName = className
        )
    }
}
