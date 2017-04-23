import matplotlib.pyplot as plt
import subprocess

#[1, 1, 1, 0.1, 0.99, 2000]
	
i_vals = [1,4]
m_vals = [1,1.5]
n_vals = [0.5,1]
f_vals = [0.1,0.3]
s_vals = [0.01, 0.0001]

T = 1000

count = 0

for i in i_vals:
	for m in m_vals:
		for n in n_vals:
			for f in f_vals:
				for s in s_vals:
					subprocess.check_output(["java", "TCPcc", "-i", str(i), "-m", str(m), "-n", str(n), "-f", str(f), "-s", str(s), "-T", str(T), "-o", "output.txt"])
					file = open("output.txt", "r")
					data = file.read()
					file.close()

					x = []
					points = []
					for p in data.split("\n")[0:-1]:
						t = p.split("|")
						x.append(t[0])
						points.append(t[1])

					plt.plot(x, points)
					plt.title("i:"+str(i)+", m:"+str(m)+", n:"+str(n)+", f:"+str(f)+", s:"+str(s))
					plt.xlabel("Round number")
					plt.ylabel("Congestion Window")
					count += 1
					plt.savefig("file"+str(count)+".png")
					plt.gcf().clear()