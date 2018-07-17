# Testing metric collector program

This document contains possible test cases for "collect-pref-metrics" program.

## Functional tests

### Valid inputs

1. Should return correct data when executed for last week
	1. Open config file
	2. Fill from and until parameters with values: 
		from=-7d
		until=now
	3. Fill all other parameters with default values
	4. Execute program
	5. Compare results with expected results
 
2. Should return correct data when 'from' is specified using -<day>d format
	1. Open config file
	2. Fill from and until parameters with values: 
		from=-7d
		until=<timestamp>
	3. Fill all other parameters with default values
	4. Execute program
	5. Compare results with expected results

3. Should return correct data when 'until' is specified using <timestamp> format
	1. Open config file
	2. Fill from and until parameters with values: 
		from=<timestamp>
		until=<timestamp>
	3. Fill all other parameters with default values
	4. Execute program
	5. Compare results with expected results

4. Should return correct data when executed for previous month
	1. Open config file
	2. Fill from and until parameters with values: 
		from=1525125600
		until=1527804059
	3. Fill all other parameters with default values
	4. Execute program
	5. Compare results with expected results

5. Should return correct data when executed against not default grafana server
	1. Open config file
	2. Fill website parameters
		scheme=http
		domain=176.119.59.95
		port=3000
		path=api/datasources/proxy/1/render
	3. Fill all other parameters with default values
	4. Execute program
	5. Compare results with expected results

6. Should return correct data when executed for only 1 metric
	1. Open config file
	2. Fill amoutOfTargets parameter with value: 
		amoutOfTargets=1
	3. Fill all other parameters with default values
	4. Execute program
	5. Compare results with expected results

7. Should return correct data when executed for max number of metrics
	1. Open config file
	2. Fill amoutOfTargets parameter with max value: 
		amoutOfTargets=<max_value>
	3. Fill all other parameters with default values
	4. Execute program
	5. Compare results with expected results

8. Should return correct data when header contains special characters
	1. Open config file
	2. Fill target.1.header parameter with value: 
		target.1.header==!@#$%^&*()QWERTsdfgh=
	3. Fill all other parameters with default values
	4. Execute program
	5. Compare results with expected results

9. Should return correct data when header is empty
	1. Open config file
	2. Fill target.1.header parameter with value: 
		target.1.header=
	3. Fill all other parameters with default values
	4. Execute program
	5. Compare results with expected results

10. Should return data for values lower then 5000
	1. Open config file
	2. Fill target.1.limit parameter with value: 
		target.1.limit=5000
	3. Fill all other parameters with default values
	4. Execute program
	5. Compare results with expected results

11. Should return data for all values
	1. Open config file
	2. Fill target.1.params parameter with value: 
		target.1.params=aliasByNode(sitespeed_io.bilety_na_wydarzenia.pageSummary.*.*.chrome.native.browsertime.statistics.visualMetrics.FirstVisualChange.mean, 3)
	3. Fill all other parameters with default values
	4. Execute program
	5. Compare results with expected results

### Invalid inputs

1. Should fail with invalid domain
	1. Open config file
	2. Fill website parameters
		domain=0.0.0.0
	3. Fill all other parameters with default values
	4. Execute program

### Presenting results

1. Should return data that can be parsed by Excel
	1. Open config file
	2. Fill all parameters with default values
	3. Execute program
	4. Open generated file with results with Excel

2. Should return data with only last measurements
	1. Open config file
	2. Fill all parameters with default values
	3. Execute program
	4. Open generated file with results
	5. Compare results with expected results


#### Config Entries
** config.properties
scheme		http,htpps
domain		176.119.59.95,<other-valid-grafana-ip>
port		3000,<other-valid-grafana-port>
path		api/datasources/proxy/1/render,<other-valid-grafana-path>
format		json
from		-7d,-10d,<timestamp>
until		now, -5d,<timestamp>


amoutOfTargets	4,1,<MAX>
target.1.header	First Visula Change,,=!@#$%^&*()QWERTsdfgh=,
target.1.params	aliasByNode(sitespeed_io.bilety_na_wydarzenia.pageSummary.*.*.chrome.native.browsertime.statistics.visualMetrics.FirstVisualChange.mean, 3)
target.1.limit	1000000,5000,0

** grafana
websites 	6,1,<MAX>
measurements	<average-week>,<max>,0
wykres mo¿e 
