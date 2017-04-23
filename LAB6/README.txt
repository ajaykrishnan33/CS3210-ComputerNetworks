Instructions for compilation:
make

Instructions for running:

	1. Sender:
		java Sender -s localhost -p 12345 -l 128 -r 10 -n 500 -w 3 -b 10 -d
	2. Receiver:
		java Receiver -p 12345 -l 128 -n 500 -e 0.01 -d

Instructions for running typescript:
	1. Sender:
		scriptreplay --timing=sender_timing.log sender_script.log

	2. Receiver:
		scriptreplay --timing=receiver_timing.log receiver_script.log
