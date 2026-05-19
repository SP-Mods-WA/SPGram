package com.spmods.spgram.presentation.di

import org.koin.dsl.module
import com.spmods.spgram.presentation.di.coil.coilModule

val uiModule = module {
    includes(coilModule)
}