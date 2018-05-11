/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.mill.tasks.file;

import com.metreeca.tray.IO;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

import static com.metreeca.mill.tasks.file.Excel.*;
import static com.metreeca.spec.Values.literal;
import static com.metreeca.spec.ValuesTest.term;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static java.lang.String.format;
import static java.math.RoundingMode.HALF_UP;


public final class ExcelTest {

	private static final String string="string";
	private static final String number="number";
	private static final String date="date";

	private static final int Scale=5;


	private final Random random=new Random();


	private Excel.Record target() {
		return record(
				string(term(string)),
				amount(term(number)).scale(Scale),
				date(term(date))
		);
	}

	private Map<String, Object> source() {

		final Map<String, Object> values=new LinkedHashMap<>();

		values.put(string, string+random.nextInt(100));
		values.put(number, BigDecimal.valueOf(1000*random.nextDouble()).setScale(Scale, HALF_UP).doubleValue());
		values.put(date, Date.from(Instant.now()));

		return values;
	}


	private void exec(final String name, final Consumer<Sheet> task) {
		try (final Workbook workbook=new XSSFWorkbook(IO.input(this, ".xlsx"))) {

			task.accept(workbook.getSheet(name));

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	private void exec(final String sheet, final String cell, final Consumer<Cell> task) {
		exec(sheet, _sheet -> Excel.area(_sheet, cell).ifPresent(area -> Excel.cell(_sheet, area.getFirstCell()).ifPresent(task)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testMapValues() {
		exec("Form", string, cell -> {

			final Excel.Field field=string(term(string))
					.map(value -> literal(value.stringValue().toUpperCase()));

			assertEquals("read value mapped",
					field.get(cell).stringValue().toUpperCase(), field.get(cell).stringValue());

			field.set(cell, literal(string));

			assertEquals("written value mapped",
					string.toUpperCase(), cell.getStringCellValue());

		});
	}

	@Test public void testHandleNumericScale() {
		exec("Form", number, cell -> {

			final Excel.NumberField field=amount(term(number));

			for (int n=1; n <= 3; ++n) {
				assertTrue("value read with scale "+n,
						field.scale(n).get(cell).stringValue().matches(format("\\d*\\.\\d{%d}", n)));
			}

			for (int n=1; n <= 3; ++n) {

				field.scale(n).set(cell, literal(BigDecimal.valueOf(123.4567890)));

				assertEquals("value written with scale "+n, n,
						BigDecimal.valueOf(cell.getNumericCellValue()).scale());

			}
		});
	}


	@Test(expected=IllegalArgumentException.class) public void testHandleParsingErrors() {
		exec("Table", sheet -> record(

				string(term(string)).map(value -> Excel.error("forced"))

		).parse(sheet.getRow(0), (iri, value) -> {}));
	}

	@Test(expected=IllegalArgumentException.class) public void testHandleFormattingErrors() {
		exec("Form", sheet -> record(

				string(term(string)).map(value -> Excel.error("forced"))

		).form(sheet, iri -> null));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testPopulateForm() {
		exec("Form", sheet -> {

			final Excel.Record record=target();

			final Map<String, Object> source=source();

			record.form(sheet, iri -> literal(source.get(iri.getLocalName())));

			assertEquals("string set", source.get(string), Excel.area(sheet, string)
					.map(area -> area.getAllReferencedCells()[0])
					.flatMap(reference -> Excel.cell(sheet, reference))
					.orElseThrow(() -> new NoSuchElementException("string cell"))
					.getStringCellValue());

			assertEquals("number set", source.get(number), Excel.area(sheet, number)
					.map(area -> area.getAllReferencedCells()[0])
					.flatMap(reference -> Excel.cell(sheet, reference))
					.orElseThrow(() -> new NoSuchElementException("number cell"))
					.getNumericCellValue());

			assertEquals("date set", source.get(date), Excel.area(sheet, date)
					.map(area -> area.getAllReferencedCells()[0])
					.flatMap(reference -> Excel.cell(sheet, reference))
					.orElseThrow(() -> new NoSuchElementException("date cell"))
					.getDateCellValue());

		});
	}

	@Test public void testPopulateTable() {
		exec("Table", sheet -> {

			final Excel.Record record=target();

			final List<Map<String, Object>> sources=new ArrayList<>();

			for (int n=3; n > 0; --n) { sources.add(source()); }

			record.table(sheet, sources.stream().map(source -> iri -> literal(source.get(iri.getLocalName()))));

			for (final Row row : sheet) {

				final int index=row.getRowNum();

				if ( index == 0 ) { // header

					assertEquals("string header preserved", string, row.getCell(0).getStringCellValue());
					assertEquals("number header preserved", number, row.getCell(1).getStringCellValue());
					assertEquals("date header preserved", date, row.getCell(2).getStringCellValue());

				} else {

					final Map<String, Object> source=sources.get(index-1);

					assertEquals("string field populated "+(index+1),
							source.get(string), row.getCell(0).getStringCellValue());

					assertEquals("number field populated "+(index+1),
							source.get(number), row.getCell(1).getNumericCellValue());

					assertEquals("date field populated "+(index+1),
							source.get(date), row.getCell(2).getDateCellValue());

				}
			}
		});
	}

	@Test public void testPopulateArea() {
		exec("Area", sheet -> {

			final Excel.Record record=target();

			final List<Map<String, Object>> sources=new ArrayList<>();

			for (int n=3; n > 0; --n) { sources.add(source()); }

			record.area(sheet, "range", (row, col) ->
					literal(sources.get(row).get(col == 0 ? string : col == 1 ? number : col == 2 ? date : null)));

			for (int i=0; i < 3; ++i) {

				final Map<String, Object> source=sources.get(i);

				final int rowOffset=1;
				final int colOffset=2;
				final Row row=sheet.getRow(i+rowOffset);

				assertEquals("string cell populated "+(i+rowOffset+1),
						source.get(string), row.getCell(0+colOffset).getStringCellValue());

				assertEquals("number cell populated "+(i+rowOffset+1),
						source.get(number), row.getCell(1+colOffset).getNumericCellValue());

				assertEquals("date cell populated "+(i+rowOffset+1),
						source.get(date), row.getCell(2+colOffset).getDateCellValue());
			}
		});
	}

}
