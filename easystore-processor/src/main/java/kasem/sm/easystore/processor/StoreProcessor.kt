/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ksp.writeTo
import kasem.sm.easystore.core.EasyStore
import kasem.sm.easystore.processor.generator.StoreFileGenerator

class StoreProcessor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {

    private lateinit var visitor: StoreVisitor
    private lateinit var storeFileGenerator: StoreFileGenerator

    override fun process(resolver: Resolver): List<KSAnnotated> {
        var unresolvedSymbols: List<KSAnnotated> = emptyList()
        val annotationName = EasyStore::class.qualifiedName

        if (annotationName != null) {
            val resolved = resolver
                .getSymbolsWithAnnotation(annotationName)
                .toList()

            val validatedSymbols = resolved.filter { it.validate() }.toList()

            validatedSymbols
                .filter {
                    it is KSClassDeclaration && it.validate()
                }
                .forEach {
                    visitor = StoreVisitor(logger)
                    it.accept(visitor, Unit)
                }

            if (::visitor.isInitialized) {
                storeFileGenerator = StoreFileGenerator(visitor)

                try {
                    storeFileGenerator.fileSpec.build().writeTo(codeGenerator = codeGenerator, aggregating = false)
                } catch (e: FileAlreadyExistsException) {
                    logger.logging(e.message.toString())
                } catch (e: Exception) {
                    logger.error(e.message.toString())
                }

                unresolvedSymbols = resolved - validatedSymbols.toSet()
            }
        }
        return unresolvedSymbols
    }
}
