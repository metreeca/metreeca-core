prefix toys: <terms#>

prefix owl: <http://www.w3.org/2002/07/owl#>
prefix xsd: <http://www.w3.org/2001/XMLSchema#>

#### assign the unique scale-based product code generated by the slug function ####

insert { $this toys:code $name } where {};


#### initialize stock #############################################################

insert { $this toys:stock 0 } where {};