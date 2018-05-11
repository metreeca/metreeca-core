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

import com.metreeca.mill.Task;
import com.metreeca.mill._Cell;
import com.metreeca.spec.Values;
import com.metreeca.tray.Tool;

import org.apache.poi.ss.formula.BaseFormulaEvaluator;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import static com.metreeca.jeep.Strings.normalize;
import static com.metreeca.spec.Values.literal;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;


/**
 * Excel extraction task and utilities.
 */
public final class Excel implements Task {

	/**
	 * The MIME type for Excel workbooks ({@value}).
	 */
	public static final String MIME="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	/**
	 * The default precision for currency amounts {@linkplain Field fields}
	 */
	private static final int AmountScale=2;


	//// Factories /////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Record record(final Field... fields) {
		return record(asList(fields));
	}

	public static Record record(final Collection<Field> fields) {

		if ( fields == null ) {
			throw new NullPointerException("null fields");
		}

		if ( fields.contains(null) ) {
			throw new NullPointerException("null field");
		}

		return new Record(fields);
	}


	public static IgnoredField ignored() { return new IgnoredField(); }

	public static StringField string(final IRI name) {
		return new StringField(name);
	}

	public static NumberField amount(final IRI name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return new NumberField(name, AmountScale);
	}

	public static DateField date(final IRI name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return new DateField(name);
	}


	//// Utilities /////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Converts a textual column name (e.g. {@code AB}) to a zero-base numeric index.
	 *
	 * @param column the textual column name to be converted
	 *
	 * @return the zero-based numeric index corresponding to {@code column}
	 *
	 * @throws IllegalArgumentException if {@code column} is empty or not a valid textual column name
	 */
	public static int index(final String column) throws IllegalArgumentException { // 0-based

		if ( column == null ) {
			throw new NullPointerException("null column");
		}

		if ( column.isEmpty() ) {
			throw new IllegalArgumentException("empty column");
		}

		return CellReference.convertColStringToIndex(column);
	}

	/**
	 * Converts a zero-based numeric column index to a textual column name (e.g. {@code AB})
	 *
	 * @param index the zero-based numeric index to be converted
	 *
	 * @return the textual colum name corresponding to {@code index}
	 *
	 * @throws IllegalArgumentException if {@code index} is negative
	 */
	public static String column(final int index) throws IllegalArgumentException { // 0-based

		if ( index < 0 ) {
			throw new IllegalArgumentException("negative index");
		}

		return CellReference.convertNumToColString(index);
	}


	/**
	 * Retrieves the raw context of a cell.
	 *
	 * @param cell the cell whose content is to be retrieved
	 *
	 * @return the raw textual content of {@code cell}
	 */
	public static String raw(final Cell cell) {
		return cell == null ? "" : normalize(new DataFormatter().formatCellValue(cell));
	}


	/**
	 * Retrieves a named area reference from a sheet.
	 *
	 * @param sheet the sheet the named area reference is to be retrieved from
	 * @param name  the name of the area reference to be retrieved
	 *
	 * @return an optional reference to the target named area or an empty optional if no area with the given {@code
	 * name} exists in the target {@code sheet}
	 */
	public static Optional<AreaReference> area(final Sheet sheet, final String name) {

		if ( sheet == null ) {
			throw new NullPointerException("null sheet");
		}

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		final Workbook workbook=sheet.getWorkbook();
		final int index=workbook.getSheetIndex(sheet.getSheetName());

		return workbook.getNames(name).stream()
				.filter(n -> n.getSheetIndex() < 0 || n.getSheetIndex() == index)
				.findFirst()
				.map(Name::getRefersToFormula)
				.map(formula -> new AreaReference(formula, workbook.getSpreadsheetVersion()));
	}

	/**
	 * Retrieves a cell from a sheet.
	 *
	 * @param sheet     the sheet the cell is to be retrieved from
	 * @param reference a reference to the cell to be retrieved
	 *
	 * @return an optional cell or an empty optional if no cell corresponding to the given {@code reference} exists in
	 * the target {@code sheet}
	 */
	public static Optional<Cell> cell(final Sheet sheet, final CellReference reference) {

		if ( sheet == null ) {
			throw new NullPointerException("null workbook");
		}

		if ( reference == null ) {
			throw new NullPointerException("null reference");
		}

		return Optional.of(sheet)
				.map(s -> s.getRow(reference.getRow()))
				.map(r -> r.getCell(reference.getCol()));
	}


	/**
	 * Evaluate formulas in a workbook.
	 *
	 * @param workbook the workbook whose formulas are to be evaluated
	 *
	 * @return the target {@code workbook}
	 */
	public static Workbook evaluate(final Workbook workbook) {

		if ( workbook == null ) {
			throw new NullPointerException("null workbook");
		}

		BaseFormulaEvaluator.evaluateAllFormulaCells(workbook);

		return workbook;
	}


	public static Value error(final String cause) {
		throw new IllegalArgumentException(cause);
	}

	private static RuntimeException error(final Field field, final Cell cell, final Throwable cause) {
		return new IllegalArgumentException("conversion error at cell "
				+cell.getAddress().formatAsString()+" ("+field.name().getLocalName()+") : "
				+Optional.ofNullable(cause.getMessage()).orElseGet(cause::toString), cause);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Stream<_Cell> execute(final Tool.Loader tools, final Stream<_Cell> items) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class Record {

		private final List<Field> fields;


		private Record(final Collection<Field> fields) {
			this.fields=new ArrayList<>(fields);
		}


		public List<Field> fields() {
			return unmodifiableList(fields);
		}


		//// Readers ///////////////////////////////////////////////////////////////////////////////////////////////////

		public Record parse(final Row row, final BiConsumer<IRI, Value> target) {

			if ( row == null ) {
				throw new NullPointerException("null row");
			}

			if ( target == null ) {
				throw new NullPointerException("null target");
			}

			for (int i=0; i < fields.size(); i++) {

				final Field field=fields.get(i);

				final Cell cell=row.getCell(i);

				if ( cell != null ) {

					try {

						final Value value=field.get(cell);

						if ( value != null ) {
							target.accept(field.name(), value);
						}

					} catch ( final RuntimeException e ) {

						throw error(field, cell, e);

					}
				}
			}

			return this;
		}


		//// Writers ///////////////////////////////////////////////////////////////////////////////////////////////////

		public Record form(final Sheet sheet, final TupleQuery query) {

			if ( sheet == null ) {
				throw new NullPointerException("null sheet");
			}

			if ( query == null ) {
				throw new NullPointerException("null query");
			}

			try (final Stream<BindingSet> results=Iterations.stream(query.evaluate())) {

				results.findFirst().ifPresent(bindings -> form(sheet, name -> bindings.getValue(name.getLocalName()))); // !!! handle name clashes

			}

			return this;
		}

		public Record form(final Sheet sheet, final Function<IRI, Value> source) {

			if ( sheet == null ) {
				throw new NullPointerException("null sheet");
			}

			if ( source == null ) {
				throw new NullPointerException("null source");
			}

			for (final Field field : fields) {
				Excel.area(sheet, field.name().getLocalName()).ifPresent(area -> {
					for (final CellReference reference : area.getAllReferencedCells()) {
						cell(sheet, reference).ifPresent(cell -> {
							try {

								field.set(cell, source.apply(field.name()));

							} catch ( final RuntimeException e ) {

								throw error(field, cell, e);

							}
						});
					}
				});
			}

			return this;
		}


		public Record table(final Sheet sheet, final TupleQuery query) {

			if ( sheet == null ) {
				throw new NullPointerException("null sheet");
			}

			if ( query == null ) {
				throw new NullPointerException("null query");
			}

			try (final Stream<BindingSet> results=Iterations.stream(query.evaluate())) {

				return table(sheet, results.map(bindings -> name -> bindings.getValue(name.getLocalName()))); // !!! handle name clashes

			}
		}

		public Record table(final Sheet sheet, final Stream<Function<IRI, Value>> sources) {

			if ( sheet == null ) {
				throw new NullPointerException("null sheet");
			}

			if ( sources == null ) {
				throw new NullPointerException("null source");
			}

			try {

				final Row template=sheet.getRow(sheet.getLastRowNum());

				final short rowHeight=template.getHeight();
				final CellStyle rowStyle=template.getRowStyle();

				final List<CellStyle> colStyles=new ArrayList<>();

				for (final Cell cell : template) {
					colStyles.add(cell.getCellStyle());
				}

				sheet.removeRow(template);

				sources.forEachOrdered(source -> {

					if ( source == null ) {
						throw new NullPointerException("null source");
					}

					final Row row=sheet.createRow(sheet.getLastRowNum()+1); // 0-based

					row.setHeight(rowHeight); // !!! exclude to auto-fit rows
					row.setRowStyle(rowStyle);

					for (final Field field : fields) {

						final Cell cell=row.createCell(Math.max(0, row.getLastCellNum())); // ;( already 1-based

						cell.setCellStyle(colStyles.get(cell.getColumnIndex()));

						try {

							field.set(cell, source.apply(field.name()));

						} catch ( final RuntimeException e ) {

							throw error(field, cell, e);

						}

					}

				});

				return this;

			} finally {
				sources.close();
			}
		}


		public Record area(final Sheet sheet, final String name, final BiFunction<Integer, Integer, Value> source) {

			if ( sheet == null ) {
				throw new NullPointerException("null sheet");
			}

			if ( name == null ) {
				throw new NullPointerException("null name");
			}

			if ( source == null ) {
				throw new NullPointerException("null source");
			}

			Excel.area(sheet, name).ifPresent(area -> {

				final CellReference origin=area.getFirstCell();

				for (final CellReference reference : area.getAllReferencedCells()) {

					final int row=reference.getRow()-origin.getRow();
					final int col=reference.getCol()-origin.getCol();

					final Field field=fields.get(col);
					final Value value=source.apply(row, col);

					cell(sheet, reference).ifPresent(cell -> {
						try {

							field.set(cell, value);

						} catch ( final RuntimeException e ) {

							throw error(field, cell, e);

						}
					});

				}

			});

			return this;
		}

	}


	public static interface Field {

		// !!! reauired()
		// !!! well-formedness rules

		public IRI name();

		public Value get(final Cell cell);

		public void set(final Cell cell, final Value value);


		public default Field map(final UnaryOperator<Value> mapper) {

			if ( mapper == null ) {
				throw new NullPointerException("null mapper");
			}

			return new DelegateField(this) {

				@Override public Value get(final Cell cell) {
					return mapper.apply(super.get(cell));
				}

				@Override public void set(final Cell cell, final Value value) {
					super.set(cell, mapper.apply(value));
				}

			};
		}

	}

	private abstract static class DelegateField implements Field {

		private final Field delegate;


		protected DelegateField(final Field delegate) {

			if ( delegate == null ) {
				throw new NullPointerException("null delegate");
			}

			this.delegate=delegate;
		}


		@Override public IRI name() {
			return delegate.name();
		}

		@Override public Value get(final Cell cell) {
			return delegate.get(cell);
		}

		@Override public void set(final Cell cell, final Value value) {
			delegate.set(cell, value);
		}

	}


	public static final class IgnoredField implements Field {

		@Override public IRI name() { return RDF.NIL; }


		@Override public Value get(final Cell cell) { return null; }

		@Override public void set(final Cell cell, final Value value) {}

	}

	public static final class StringField implements Field {

		private final IRI name;


		private StringField(final IRI name) { this.name=name; }


		@Override public IRI name() { return name; }

		@Override public Value get(final Cell cell) {

			final String value=raw(cell);

			return value.isEmpty() ? null : literal(value);
		}

		@Override public void set(final Cell cell, final Value value) {
			if ( value != null ) {
				cell.setCellValue(value.stringValue());
			}
		}

	}

	public static final class NumberField implements Field {

		private final IRI name;
		private final int scale; // !!! make optional


		private NumberField(final IRI name, final int scale) {
			this.name=name;
			this.scale=scale;
		}


		@Override public IRI name() { return name; }


		@Override public Value get(final Cell cell) {
			return cell.getCellTypeEnum() == CellType.NUMERIC
					? literal(BigDecimal.valueOf(cell.getNumericCellValue()).setScale(scale, RoundingMode.HALF_UP))
					: null;
		}

		@Override public void set(final Cell cell, final Value value) {
			if ( value instanceof Literal ) { // !!! handle non-numeric literals
				cell.setCellValue(((Literal)value).decimalValue().setScale(scale, RoundingMode.HALF_UP).doubleValue());
			}
		}


		public NumberField scale(final int scale) {
			return new NumberField(name, scale);
		}

	}

	public static final class DateField implements Field {

		private final IRI name;


		private DateField(final IRI name) { this.name=name; }


		@Override public IRI name() { return name; }

		@Override public Value get(final Cell cell) {
			return cell.getCellTypeEnum() == CellType.NUMERIC
					? literal(LocalDateTime.ofInstant(cell.getDateCellValue().toInstant(), ZoneId.systemDefault()))
					: null;
		}

		@Override public void set(final Cell cell, final Value value) {
			if ( value instanceof Literal ) {

				final Instant instant=Values.instant((Literal)value); // !!! nullable?

				cell.setCellValue(Date.from(instant));

			}
		}

	}


	//// !!! ///////////////////////////////////////////////////////////////////////////////////////////////////////////

	//private static final class XSLParser {
	//
	//	private final InputStream stream;
	//
	//	private XSLParser(final InputStream stream) {this.stream=stream;}
	//
	//
	//	public <E extends Exception> void parse(final XLSHandler<E> handler) throws E, IOException {
	//
	//		try (final Workbook workbook=new HSSFWorkbook(stream)) {
	//
	//			handler.startWorkbook();
	//
	//			for (final Sheet sheet : workbook) {
	//
	//				handler.startSheet(sheet.getSheetName());
	//
	//				for (final Row row : sheet) {
	//
	//					handler.startRow();
	//
	//					for (final Cell cell : row) {
	//						switch ( cell.getCellType() ) {
	//
	//							case Cell.CELL_TYPE_BOOLEAN:
	//
	//								handler.cell(Boolean.class, cell.getBooleanCellValue());
	//								break;
	//
	//							case Cell.CELL_TYPE_NUMERIC:
	//
	//								handler.cell(Number.class, cell.getNumericCellValue());
	//								break;
	//
	//							case Cell.CELL_TYPE_STRING:
	//
	//								handler.cell(String.class, cell.getStringCellValue());
	//								break;
	//
	//							case Cell.CELL_TYPE_BLANK:
	//
	//								handler.cell(Void.class, null);
	//								break;
	//
	//							case Cell.CELL_TYPE_ERROR:
	//
	//								handler.cell(Exception.class, new Exception(cell.getStringCellValue()));
	//								break;
	//
	//							default:
	//
	//								handler.cell(String.class, cell.getStringCellValue());
	//								break;
	//
	//						}
	//					}
	//
	//					handler.endRow();
	//				}
	//
	//				handler.endSheet();
	//			}
	//
	//			handler.endWorkbook();
	//
	//		}
	//	}
	//}

}
