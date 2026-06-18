package com.example.sellerhelperebay.domain.model

enum class EntryStatus { NEW, NEEDS_REVIEW, ANALYZED, PUSHED }

enum class PhotoMatchStatus { UNANALYZED, MATCHED, MISMATCH_PENDING, USER_CONFIRMED_SAME, USER_MARKED_LOT }

enum class Provenance { AI, MANUAL, WEB }
