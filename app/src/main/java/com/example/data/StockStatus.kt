package com.example.data

enum class StockStatus(val label: String) {
  NORMAL("In Stock"),
  LOW_STOCK("LOW STOCK"),
  OUT_OF_STOCK("OUT OF STOCK"),
}

fun calculateStockStatus(stockQuantity: Int, minimumStock: Int): StockStatus =
  when {
    stockQuantity <= 0 -> StockStatus.OUT_OF_STOCK
    stockQuantity <= minimumStock -> StockStatus.LOW_STOCK
    else -> StockStatus.NORMAL
  }
