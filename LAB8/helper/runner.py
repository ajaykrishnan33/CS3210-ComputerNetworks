import subprocess
from app.models import TableEntry

def pot():
    sack_vals = [0, 1]
    window_size = [16, 256] # KB
    cong_window_schemes = ['reno', 'cubic']
    link_delay = [2, 50] # 50 ms
    link_drop_percent = [0.5, 5] # %
    files = [512, 1000, 2000]
    table = []

    # ip_addr = "localhost"
    ip_addr = "192.168.0.108"

    # f = open("result.csv",  "w")
    # f.write("SACK,WINDOW SIZE,CONG WINDOW SCHEME,LINK DELAY,LINK DROP,FILE SIZE,SPEED,TIME\n")
    TableEntry.objects.all().delete()

    for s in sack_vals:
        subprocess.call('sudo sysctl -w net.ipv4.tcp_sack="'+str(s)+'"', shell=True)
        for w in window_size:
            subprocess.call('sudo sysctl -w net.ipv4.tcp_window_scaling="1"', shell=True)
            subprocess.call('sudo sysctl -w net.core.rmem_max='+str(w*1024), shell=True) ## bytes

            for c in cong_window_schemes:
                subprocess.call("sudo sysctl -w net.ipv4.tcp_congestion_control="+str(c), shell=True)
                for l in link_delay:
                    subprocess.call("sudo tc qdisc replace dev wlan0 root netem delay " + str(l) + "ms 1ms 25%", shell=True)
                    for p in link_drop_percent:
                        subprocess.call("sudo tc qdisc replace dev eth0 root netem loss " + str(p) +"% 25%", shell=True)

                        for i in range(len(files)):
                            sz = files[i]
                            speed = 0.0
                            time = 0.0
                            for j in range(5):
                                output = subprocess.check_output(["wget", "-P", "output/", ip_addr+":5000/testfiles/file"+str(sz)], stderr=subprocess.STDOUT)
                                arr = output.split("MB")
                                flag = False
                                if len(arr)==1:
                                    flag = True
                                    arr = output.split("KB")

                                a = range(len(arr[0]))
                                a.reverse()
                                x = ''
                                for i in a:
                                    if arr[0][i]=='(':
                                        break
                                    x += arr[0][i]
                                x = x[::-1]
                                if flag:
                                    speed += float(x[:-1])
                                else:
                                    speed += float(x[:-1])*1024.0

                                arr = output.split("=")
                                a = range(len(arr[1]))
                                x = ''
                                for i in a:
                                    # print arr[j][i]
                                    if arr[1][i]=='s':
                                        break
                                    x += arr[1][i]
                                time += float(x)

                            speed = speed/5.0
                            time = time/5.0

                            table.append([s,w,c,l,p,sz,speed,time])
                            TableEntry.objects.create(sack=s, window_size=w, cong_window_schemes=c,link_delay=l,link_drop_percent=p,file_size=sz,speed=speed,time=time)

                            # f.write(str(s)+","+str(w)+","+str(c)+","+str(l)+","+str(p)+","+str(sz)+","+str(speed)+","+str(time)+"\n")

    # f.close()

pot()