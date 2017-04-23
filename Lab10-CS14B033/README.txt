For compilation:
	make

For running:
	java OSPFRouter -i <node_id> -f <infile> -o <outfile> -h <hello_interval> -a <lsa_interval> -s <spf_interval>

For bulk running:
	python runner.py

For killing bulk runner:
	kill -9 `pgrep -f OSPFRouter`