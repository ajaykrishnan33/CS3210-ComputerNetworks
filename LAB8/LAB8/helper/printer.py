from app.models import TableEntry

for sz in [512, 1000, 2000]:
	f = open("results"+str(sz)+".csv", "w")
	f.write("SACK,WINDOW SIZE,CONG WINDOW SCHEME,LINK DELAY,LINK DROP,SPEED,TIME\n")
	for t in TableEntry.objects.filter(file_size=sz):
		s = t.sack
		w = t.window_size
		c = t.cong_window_schemes
		l = t.link_delay
		p = t.link_drop_str
		speed = t.speed/1024.0
		time = t.time
		f.write(str(s)+","+str(w)+","+str(c)+","+str(l)+","+str(p)+","+str(speed)+","+str(time)+"\n")

	f.close()