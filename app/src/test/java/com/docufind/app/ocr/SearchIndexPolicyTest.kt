package com.docufind.app.ocr

import com.docufind.app.security.SearchIndexPolicy
import org.junit.Assert.assertFalse
import org.junit.Test

class SearchIndexPolicyTest {

    @Test
    fun allowsOcrTextInSearchIndex_isFalse() {
        assertFalse(SearchIndexPolicy.allowsOcrTextInSearchIndex())
    }
}
