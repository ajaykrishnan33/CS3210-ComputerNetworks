import subprocess
import matplotlib.pyplot as plt

N = 50
probs = [0.01, 0.02, 0.03, 0.05, 0.1]
M = 10000

R = 10000 # max retransmissions are R, default 10

# probs = [0.001*i for i in range(1,101)] ## probs for N=1000

utils2 = []
utils4 = []

for p in probs:
	utilisation = 0.0
	repeat = 10
	for i in range(repeat):
		x = subprocess.check_output(["java", "SlottedAloha", "-N", str(N), "-W", str(2), "-p", str(p), "-M", str(M), "-A", "-R", str(R)])
		x = x.strip().split(" ")
		utilisation += float(x[0]) 

	utils2.append(utilisation/repeat)

	utilisation = 0.0
	for i in range(repeat):
		x = subprocess.check_output(["java", "SlottedAloha", "-N", str(N), "-W", str(4), "-p", str(p), "-M", str(M), "-A", "-R", str(R)])
		x = x.strip().split(" ")
		utilisation += float(x[0])

	utils4.append(utilisation/repeat)

plt.plot(probs, utils2, 'r', label='W=2')
plt.plot(probs, utils4, 'g', label='W=4')
plt.legend(loc='lower right')
plt.ylabel('Utilization(Throughput)')
plt.xlabel('Packet generation probability')
plt.title('Utilization vs Packet generation probability for the Slotted Aloha')
plt.show()


