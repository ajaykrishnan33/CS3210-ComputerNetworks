import subprocess

# for i in range(20):
# 	subprocess.Popen(["java", "OSPFRouter", "-i", str(i), "-f", "input1.txt", "-o", "outputs1/output"+str(i)+".txt"])

# for i in range(14):
# 	subprocess.Popen(["java", "OSPFRouter", "-i", str(i), "-f", "input2.txt", "-o", "outputs2/output"+str(i)+".txt"])

for i in range(6):
	subprocess.Popen(["java", "OSPFRouter", "-i", str(i), "-f", "input.txt", "-o", "outputs/output"+str(i)+".txt"])