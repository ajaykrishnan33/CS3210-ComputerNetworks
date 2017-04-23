For compilation:
	make

For running the program:
	java TCPcc -i <initial cw> -m <exponential multiplier> -n <linear multiplier> -f <cw fraction on timeout> -s <probability of timeout> -T <number of packets> -o <output file name>

	eg.
	java TCPcc -i 1 -m 1 -n 1 -f 0.1 -s 0.01 -T 1000 -o output.txt

For generating the graphs:
	make
	python script.py
