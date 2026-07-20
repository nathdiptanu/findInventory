package com.docufind.app.domain.model.support

import com.docufind.app.domain.model.module.DocuFindModule

object SupportModuleOptions {
    val options: List<String> = buildList {
        add("General")
        addAll(DocuFindModule.coreModules.map { it.title })
        add("Search")
        add("Reminders")
        add("Settings")
        add("Backup & Restore")
        add("Family Members")
        add("Pets")
        add("Other")
    }
}
