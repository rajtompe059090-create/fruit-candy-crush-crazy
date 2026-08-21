package com.fruitcandycrushcarzy.APP.game.logic

import com.fruitcandycrushcarzy.APP.game.model.Fruit
import com.fruitcandycrushcarzy.APP.game.model.Position
import com.fruitcandycrushcarzy.APP.game.model.SpecialType

object GameLogic {

    // ==========================================
    // 6 x 6 CANDY CRUSH BOARD
    // ==========================================

    const val GRID_SIZE = 6

    // ==========================================
    // CREATE SAFE INITIAL BOARD
    // ==========================================

    fun createInitialGrid(): Array<Array<Fruit?>> {

        val grid = Array(GRID_SIZE) {
            arrayOfNulls<Fruit>(GRID_SIZE)
        }

        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {

                var fruit: Fruit

                var attempts = 0

                do {

                    fruit = Fruit.random()
                    attempts++

                } while (
                    wouldCreateMatch(
                        grid,
                        r,
                        c,
                        fruit
                    ) && attempts < 100
                )

                grid[r][c] = fruit
            }
        }

        return grid
    }

    private fun wouldCreateMatch(
        grid: Array<Array<Fruit?>>,
        row: Int,
        col: Int,
        fruit: Fruit
    ): Boolean {

        // Horizontal 3
        if (
            col >= 2 &&
            grid[row][col - 1]?.type == fruit.type &&
            grid[row][col - 2]?.type == fruit.type
        ) {
            return true
        }

        // Vertical 3
        if (
            row >= 2 &&
            grid[row - 1][col]?.type == fruit.type &&
            grid[row - 2][col]?.type == fruit.type
        ) {
            return true
        }

        return false
    }

    // ==========================================
    // FIND MATCH GROUPS
    // ==========================================

    fun findMatchGroups(
        grid: Array<Array<Fruit?>>
    ): List<List<Position>> {

        val groups =
            mutableListOf<List<Position>>()

        // --------------------------------------
        // HORIZONTAL
        // --------------------------------------

        for (r in 0 until GRID_SIZE) {

            var start = 0

            while (start < GRID_SIZE) {

                val fruit =
                    grid[r][start]

                if (fruit == null) {
                    start++
                    continue
                }

                var end = start + 1

                while (
                    end < GRID_SIZE &&
                    grid[r][end]?.type == fruit.type
                ) {
                    end++
                }

                val count =
                    end - start

                if (count >= 3) {

                    groups.add(
                        (start until end).map { c ->
                            Position(r, c)
                        }
                    )
                }

                start = end
            }
        }

        // --------------------------------------
        // VERTICAL
        // --------------------------------------

        for (c in 0 until GRID_SIZE) {

            var start = 0

            while (start < GRID_SIZE) {

                val fruit =
                    grid[start][c]

                if (fruit == null) {
                    start++
                    continue
                }

                var end = start + 1

                while (
                    end < GRID_SIZE &&
                    grid[end][c]?.type == fruit.type
                ) {
                    end++
                }

                val count =
                    end - start

                if (count >= 3) {

                    groups.add(
                        (start until end).map { r ->
                            Position(r, c)
                        }
                    )
                }

                start = end
            }
        }

        return groups
    }

    // ==========================================
    // SPECIAL / L / T / LINE EFFECTS
    // ==========================================

    fun getAffectedPositions(
        grid: Array<Array<Fruit?>>,
        matches: Set<Position>
    ): Set<Position> {

        val affected =
            matches.toMutableSet()

        val queue =
            matches.toMutableList()

        val checked =
            mutableSetOf<Position>()

        while (queue.isNotEmpty()) {

            val pos =
                queue.removeAt(0)

            if (!checked.add(pos)) {
                continue
            }

            val fruit =
                grid.getOrNull(pos.row)
                    ?.getOrNull(pos.col)
                    ?: continue

            when (fruit.special) {

                // ----------------------------------
                // ROW BLAST
                // ----------------------------------

                SpecialType.ROW_BLAST -> {

                    for (c in 0 until GRID_SIZE) {

                        val p =
                            Position(
                                pos.row,
                                c
                            )

                        if (affected.add(p)) {
                            queue.add(p)
                        }
                    }
                }

                // ----------------------------------
                // COLUMN BLAST
                // ----------------------------------

                SpecialType.COL_BLAST -> {

                    for (r in 0 until GRID_SIZE) {

                        val p =
                            Position(
                                r,
                                pos.col
                            )

                        if (affected.add(p)) {
                            queue.add(p)
                        }
                    }
                }

                // ----------------------------------
                // BOMB
                // 3x3 AREA
                // ----------------------------------

                SpecialType.BOMB -> {

                    for (
                        r in
                        (pos.row - 1)..(pos.row + 1)
                    ) {

                        for (
                            c in
                            (pos.col - 1)..(pos.col + 1)
                        ) {

                            if (
                                r in 0 until GRID_SIZE &&
                                c in 0 until GRID_SIZE
                            ) {

                                val p =
                                    Position(r, c)

                                if (affected.add(p)) {
                                    queue.add(p)
                                }
                            }
                        }
                    }
                }

                SpecialType.NONE -> Unit
            }
        }

        return affected
    }

    // ==========================================
    // DETECT L / T SHAPE
    // ==========================================

    fun findSpecialShapePositions(
        grid: Array<Array<Fruit?>>,
        matches: List<List<Position>>
    ): List<Triple<Position, com.fruitcandycrushcarzy.APP.game.model.FruitType, SpecialType>> {

        val result =
            mutableListOf<
                Triple<
                    Position,
                    com.fruitcandycrushcarzy.APP.game.model.FruitType,
                    SpecialType
                >
            >()

        val allMatched =
            matches.flatten().toSet()

        if (allMatched.size < 5) {
            return result
        }

        val byType =
            allMatched.groupBy { position ->
                grid[position.row][position.col]?.type
            }

        byType.forEach { (_, positions) ->

            if (positions.size < 5) {
                return@forEach
            }

            val rows =
                positions.groupBy { it.row }

            val cols =
                positions.groupBy { it.col }

            val hasHorizontal =
                rows.any { it.value.size >= 3 }

            val hasVertical =
                cols.any { it.value.size >= 3 }

            if (
                hasHorizontal &&
                hasVertical
            ) {

                val center =
                    positions.first()

                val fruit =
                    grid[
                        center.row
                    ][
                        center.col
                    ] ?: return@forEach

                result.add(
                    Triple(
                        center,
                        fruit.type,
                        SpecialType.BOMB
                    )
                )
            }
        }

        return result
    }

    // ==========================================
    // GRAVITY
    // ==========================================

    fun applyGravity(
        grid: Array<Array<Fruit?>>
    ): List<Pair<Position, Position>> {

        val movements =
            mutableListOf<Pair<Position, Position>>()

        for (c in 0 until GRID_SIZE) {

            var writeRow =
                GRID_SIZE - 1

            for (
                r in GRID_SIZE - 1 downTo 0
            ) {

                val fruit =
                    grid[r][c]

                if (fruit != null) {

                    if (r != writeRow) {

                        movements.add(
                            Position(r, c) to
                                Position(writeRow, c)
                        )

                        grid[writeRow][c] =
                            fruit

                        grid[r][c] = null
                    }

                    writeRow--
                }
            }
        }

        return movements
    }

    // ==========================================
    // REFILL
    // ==========================================

    fun refillGrid(
        grid: Array<Array<Fruit?>>
    ): List<Pair<Fruit, Position>> {

        val newFruits =
            mutableListOf<Pair<Fruit, Position>>()

        for (c in 0 until GRID_SIZE) {

            for (r in 0 until GRID_SIZE) {

                if (grid[r][c] == null) {

                    val fruit =
                        Fruit.random()

                    grid[r][c] =
                        fruit

                    newFruits.add(
                        fruit to
                            Position(r, c)
                    )
                }
            }
        }

        return newFruits
    }

    // ==========================================
    // ADJACENT
    // ==========================================

    fun isAdjacent(
        p1: Position,
        p2: Position
    ): Boolean {

        return (
            kotlin.math.abs(
                p1.row - p2.row
            ) == 1 &&
                p1.col == p2.col
            ) || (
            kotlin.math.abs(
                p1.col - p2.col
            ) == 1 &&
                p1.row == p2.row
            )
    }

    // ==========================================
    // AVAILABLE MOVE CHECK
    // ==========================================

    fun hasAvailableMoves(
        grid: Array<Array<Fruit?>>
    ): Boolean {

        for (r in 0 until GRID_SIZE) {

            for (c in 0 until GRID_SIZE) {

                // RIGHT
                if (
                    c < GRID_SIZE - 1 &&
                    checkSwapMatch(
                        grid,
                        r,
                        c,
                        r,
                        c + 1
                    )
                ) {
                    return true
                }

                // DOWN
                if (
                    r < GRID_SIZE - 1 &&
                    checkSwapMatch(
                        grid,
                        r,
                        c,
                        r + 1,
                        c
                    )
                ) {
                    return true
                }
            }
        }

        return false
    }

    // ==========================================
    // CHECK SWAP
    // ==========================================

    private fun checkSwapMatch(
        grid: Array<Array<Fruit?>>,
        r1: Int,
        c1: Int,
        r2: Int,
        c2: Int
    ): Boolean {

        val copy =
            Array(GRID_SIZE) { r ->
                Array<Fruit?>(
                    GRID_SIZE
                ) { c ->
                    grid[r][c]
                }
            }

        val temp =
            copy[r1][c1]

        copy[r1][c1] =
            copy[r2][c2]

        copy[r2][c2] =
            temp

        return findMatchGroups(copy)
            .isNotEmpty()
    }

    // ==========================================
    // FIND MATCH AT
    // ==========================================

    private fun checkMatchAt(
        grid: Array<Array<Fruit?>>,
        r: Int,
        c: Int,
        fruit: Fruit,
        skipR: Int,
        skipC: Int,
        horizontal: Boolean = true
    ): Boolean {

        var count = 1

        if (horizontal) {

            var i = c - 1

            while (i >= 0) {

                if (
                    i == skipC &&
                    r == skipR
                ) {
                    break
                }

                if (
                    grid[r][i]?.type ==
                    fruit.type
                ) {
                    count++
                    i--
                } else {
                    break
                }
            }

            i = c + 1

            while (i < GRID_SIZE) {

                if (
                    i == skipC &&
                    r == skipR
                ) {
                    break
                }

                if (
                    grid[r][i]?.type ==
                    fruit.type
                ) {
                    count++
                    i++
                } else {
                    break
                }
            }

        } else {

            var i = r - 1

            while (i >= 0) {

                if (
                    i == skipR &&
                    c == skipC
                ) {
                    break
                }

                if (
                    grid[i][c]?.type ==
                    fruit.type
                ) {
                    count++
                    i--
                } else {
                    break
                }
            }

            i = r + 1

            while (i < GRID_SIZE) {

                if (
                    i == skipR &&
                    c == skipC
                ) {
                    break
                }

                if (
                    grid[i][c]?.type ==
                    fruit.type
                ) {
                    count++
                    i++
                } else {
                    break
                }
            }
        }

        return count >= 3
    }
}
