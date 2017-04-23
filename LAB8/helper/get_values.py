from app.models import TableEntry

def get_avg(query):
	file_size = [512, 1000, 2000]
	res = {}
	for f in file_size:
		query["file_size"] = f
		avg_speed = 0
		avg_time = 0
		arr = TableEntry.objects.filter(**query)
		for t in arr:
			avg_speed += t.speed
			avg_time += t.time
		avg_speed /= len(arr)
		avg_time /= len(arr)
		res[f] = (avg_speed, avg_time)

	return res

queries = {
	"sack":[0,1],
	"window_size":[16,256],
	"cong_window_schemes":["reno","cubic"],
	"link_delay": [2,50],
	"link_drop_str": ["0.5","5.0"]
}

results = {
	"sack":{},
	"window_size":{},
	"cong_window_schemes":{},
	"link_delay": {},
	"link_drop_str": {}	
}


import matplotlib.pyplot as plt

for q in queries:
	for p in queries[q]:
		r = {}
		r[q] = p
		res = get_avg(r)
		for f in res:
			if not f in results[q]:
				results[q][f] = {"speed":[],"time":[]}

			results[q][f]["speed"].append(res[f][0]/1024);
			results[q][f]["time"].append(res[f][1]);
		# print q +"="+str(p)
		# for f in res:
		# 	print "\tSize:"+str(f)+ " avg_speed:"+str(res[f][0]/1024)+" MB/s, avg_time:"+str(res[f][1])+" s"

# plt.plot(queries["sack"], results["sack"][512]["speed"], 'r', label="512 KB")
# plt.plot(queries["sack"], results["sack"][1000]["speed"], 'b', label="1024 KB")
# plt.plot(queries["sack"], results["sack"][2000]["speed"], 'g', label="2048 KB")
# plt.title("SACK ON/OFF")
# plt.ylabel('Throughput in MB/s')
# plt.legend()
# plt.show()

# plt.plot(queries["window_size"], results["window_size"][512]["speed"], 'r', label="512 KB")
# plt.plot(queries["window_size"], results["window_size"][1000]["speed"], 'b', label="1024 KB")
# plt.plot(queries["window_size"], results["window_size"][2000]["speed"], 'g', label="2048 KB")
# plt.title("WINDOW SIZE")
# plt.ylabel('Throughput in MB/s')
# plt.legend()
# plt.show()

# queries["cong_window_schemes"] = [0,1]

# plt.plot(queries["cong_window_schemes"], results["cong_window_schemes"][512]["speed"], 'r', label="512 KB")
# plt.plot(queries["cong_window_schemes"], results["cong_window_schemes"][1000]["speed"], 'b', label="1024 KB")
# plt.plot(queries["cong_window_schemes"], results["cong_window_schemes"][2000]["speed"], 'g', label="2048 KB")
# plt.title("CONGESTION WINDOW SCHEMES")
# plt.xticks([0,1],["reno", "cubic"])
# plt.ylabel('Throughput in MB/s')
# plt.legend()
# plt.show()

plt.plot(queries["link_delay"], results["link_delay"][512]["speed"], 'r', label="512 KB")
plt.plot(queries["link_delay"], results["link_delay"][1000]["speed"], 'b', label="1024 KB")
plt.plot(queries["link_delay"], results["link_delay"][2000]["speed"], 'g', label="2048 KB")
plt.title("LINK DELAY")
plt.ylabel('Throughput in MB/s')
plt.legend()
plt.show()

# queries["link_drop_str"] = [0.5,5.0]

# plt.plot(queries["link_drop_str"], results["link_drop_str"][512]["speed"], 'r', label="512 KB")
# plt.plot(queries["link_drop_str"], results["link_drop_str"][1000]["speed"], 'b', label="1024 KB")
# plt.plot(queries["link_drop_str"], results["link_drop_str"][2000]["speed"], 'g', label="2048 KB")
# plt.title("LINK DROP PERCENTAGE")
# plt.ylabel('Throughput in MB/s')
# plt.legend()
# plt.show()

##########################
###########################

# plt.plot(queries["sack"], results["sack"][512]["time"], 'r', label="512 KB")
# plt.plot(queries["sack"], results["sack"][1000]["time"], 'b', label="1024 KB")
# plt.plot(queries["sack"], results["sack"][2000]["time"], 'g', label="2048 KB")
# plt.title("SACK ON/OFF")
# plt.ylabel('Transmission time in s')
# plt.legend()
# plt.show()

# plt.plot(queries["window_size"], results["window_size"][512]["time"], 'r', label="512 KB")
# plt.plot(queries["window_size"], results["window_size"][1000]["time"], 'b', label="1024 KB")
# plt.plot(queries["window_size"], results["window_size"][2000]["time"], 'g', label="2048 KB")
# plt.title("WINDOW SIZE")
# plt.ylabel('Transmission time in s')
# plt.legend()
# plt.show()

# queries["cong_window_schemes"] = [0,1]
# plt.plot(queries["cong_window_schemes"], results["cong_window_schemes"][512]["time"], 'r', label="512 KB")
# plt.plot(queries["cong_window_schemes"], results["cong_window_schemes"][1000]["time"], 'b', label="1024 KB")
# plt.plot(queries["cong_window_schemes"], results["cong_window_schemes"][2000]["time"], 'g', label="2048 KB")
# plt.title("CONGESTION WINDOW SCHEMES")
# plt.ylabel('Transmission time in s')
# plt.xticks([0,1],["reno", "cubic"])
# plt.legend()
# plt.show()

# plt.plot(queries["link_delay"], results["link_delay"][512]["time"], 'r', label="512 KB")
# plt.plot(queries["link_delay"], results["link_delay"][1000]["time"], 'b', label="1024 KB")
# plt.plot(queries["link_delay"], results["link_delay"][2000]["time"], 'g', label="2048 KB")
# plt.title("LINK DELAY")
# plt.ylabel('Transmission time in s')
# plt.legend()
# plt.show()

# queries["link_drop_str"] = [0.5,5.0]

# plt.plot(queries["link_drop_str"], results["link_drop_str"][512]["time"], 'r', label="512 KB")
# plt.plot(queries["link_drop_str"], results["link_drop_str"][1000]["time"], 'b', label="1024 KB")
# plt.plot(queries["link_drop_str"], results["link_drop_str"][2000]["time"], 'g', label="2048 KB")
# plt.title("LINK DROP PERCENTAGE")
# plt.ylabel('Transmission time in s')
# plt.legend()
# plt.show()


