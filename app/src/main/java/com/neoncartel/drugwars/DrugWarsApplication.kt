package com.neoncartel.drugwars

import android.app.Application
import com.neoncartel.drugwars.app.AppContainer

class DrugWarsApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
