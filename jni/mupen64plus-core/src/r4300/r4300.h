/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus - r4300.h                                                 *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2002 Hacktarux                                          *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.          *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

#ifndef M64P_R4300_R4300_H
#define M64P_R4300_R4300_H

#include "ops.h"
#include "recomp.h"

extern precomp_instr *PC;

extern int stop, llbit, rompause;
extern long long int reg[32], hi, lo;
extern long long int local_rs;
extern unsigned int delay_slot, skip_jump, dyna_interp;
extern unsigned int r4300emu;
extern unsigned int next_interupt, CIC_Chip;
extern unsigned int last_addr;
extern int no_compiled_jump;
#define COUNT_PER_OP_DEFAULT 2
extern unsigned int count_per_op;
extern cpu_instruction_table current_instruction_table;

void r4300_reset_hard(void);
void r4300_reset_soft(void);
void r4300_execute(void);

/* Jump to the given address. This works for all r4300 emulator, but is slower.
 * Use this for common code which can be executed from any r4300 emulator. */ 
void generic_jump_to(unsigned int address);

// r4300 emulators
#define CORE_PURE_INTERPRETER 0
#define CORE_INTERPRETER      1
#define CORE_DYNAREC          2

#endif /* M64P_R4300_R4300_H */

