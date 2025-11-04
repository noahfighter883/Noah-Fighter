from collections import deque
from typing import List, Tuple, Optional

# ---------- Utilities ----------

def parse_grid(grid: List[str]) -> List[int]:
    """
    Accepts 9 strings of length 9 each. Digits 1-9 are fixed; '.' or '0' are blanks.
    Returns a flat list of 81 ints in row-major order.
    """
    assert len(grid) == 9 and all(len(r) == 9 for r in grid), "Grid must be 9x9"
    vals = []
    for r in grid:
        for ch in r:
            if ch in "0.":
                vals.append(0)
            elif ch.isdigit() and ch != "0":
                d = int(ch)
                if not (1 <= d <= 9):
                    raise ValueError("Digits must be 1..9")
                vals.append(d)
            else:
                raise ValueError("Invalid char in grid")
    return vals

def box_index(r: int, c: int) -> int:
    return (r // 3) * 3 + (c // 3)

def bit(d: int) -> int:
    return 1 << (d - 1)

def bits_to_list(mask: int) -> List[int]:
    """Convert bitmask of digits (1..9) to a list of digits."""
    out = []
    for d in range(1, 10):
        if mask & bit(d):
            out.append(d)
    return out

# ---------- BFS Solver ----------

class SudokuBFS:
    def __init__(self, cells: List[int]):
        if len(cells) != 81:
            raise ValueError("Board must have 81 cells")
        self.start = cells

        # Precompute constraints as bitmasks of used digits per row/col/box.
        self.row_used = [0] * 9
        self.col_used = [0] * 9
        self.box_used = [0] * 9

        for idx, v in enumerate(cells):
            if v == 0:
                continue
            r, c = divmod(idx, 9)
            b = box_index(r, c)
            m = bit(v)
            if (self.row_used[r] & m) or (self.col_used[c] & m) or (self.box_used[b] & m):
                raise ValueError("Invalid puzzle: duplicates in row/col/box")
            self.row_used[r] |= m
            self.col_used[c] |= m
            self.box_used[b] |= m

    def _choose_cell_with_fewest_candidates(self, board: List[int],
                                            row_used: List[int],
                                            col_used: List[int],
                                            box_used: List[int]) -> Tuple[int, int]:
        """
        Return (cell_index, candidate_mask). If no blanks, returns (-1, 0).
        Chooses the blank with the smallest number of candidates (MRV).
        """
        best_idx = -1
        best_mask = 0
        best_count = 10  # more than max 9 candidates
        for idx, v in enumerate(board):
            if v != 0:
                continue
            r, c = divmod(idx, 9)
            b = box_index(r, c)
            used = row_used[r] | col_used[c] | box_used[b]
            cand_mask = (~used) & 0x1FF  # keep 9 bits
            if cand_mask == 0:
                # dead end: no candidates for this cell
                return idx, 0
            # count candidates quickly
            count = cand_mask.bit_count()
            if count < best_count:
                best_count = count
                best_idx = idx
                best_mask = cand_mask
                if best_count == 1:
                    break
        return best_idx, best_mask

    def solve(self) -> Optional[List[int]]:
        """
        Breadth-first search over states. Each state holds (board, row_used, col_used, box_used).
        Returns solved board as a flat list of 81 ints, or None if unsolvable.
        """
        # Initial state
        start_board = self.start[:]
        start_row = self.row_used[:]
        start_col = self.col_used[:]
        start_box = self.box_used[:]

        # Quick check: already solved?
        if all(v != 0 for v in start_board):
            return start_board

        q = deque()
        q.append((start_board, start_row, start_col, start_box))

        while q:
            board, row_used, col_used, box_used = q.popleft()

            # Pick the next cell with MRV to minimize branching.
            idx, cand_mask = self._choose_cell_with_fewest_candidates(board, row_used, col_used, box_used)

            if idx == -1:
                # solved
                return board

            if cand_mask == 0:
                # dead state, skip
                continue

            r, c = divmod(idx, 9)
            b = box_index(r, c)

            # Generate child states for each candidate (BFS enqueues all of them).
            # Expand candidates in ascending digit order.
            for d in range(1, 10):
                m = bit(d)
                if not (cand_mask & m):
                    continue
                # Create next state
                next_board = board[:]  # shallow copy is fine for list of ints
                next_board[idx] = d

                nr = row_used[:]    # copy constraints
                nc = col_used[:]
                nb = box_used[:]
                if (nr[r] & m) or (nc[c] & m) or (nb[b] & m):
                    # Shouldn't happen because we used cand_mask, but keep safe.
                    continue
                nr[r] |= m
                nc[c] |= m
                nb[b] |= m

                q.append((next_board, nr, nc, nb))

        return None  # unsolvable

# ---------- Pretty printing & demo ----------

def board_to_lines(board: List[int]) -> List[str]:
    out = []
    for r in range(9):
        row = board[r*9:(r+1)*9]
        out.append(" ".join(str(v) for v in row))
    return out

if __name__ == "__main__":
    # Example puzzle (dots are blanks). Feel free to replace with your own.
    puzzle = [
        "53..7....",
        "6..195...",
        ".98....6.",
        "8...6...3",
        "4..8.3..1",
        "7...2...6",
        ".6....28.",
        "...419..5",
        "....8..79",
    ]

    cells = parse_grid(puzzle)
    solver = SudokuBFS(cells)
    solved = solver.solve()
    if solved is None:
        print("No solution found.")
    else:
        print("Solved board:")
        for line in board_to_lines(solved):
            print(line)
