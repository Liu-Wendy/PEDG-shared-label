import os
import os.path
import sys

n = int(sys.argv[1])

dir="reachable/"

def mkdir(path):
	folder = os.path.exists(path)
	if not folder:
		os.makedirs(path)
		print("new folder "+path)

mkdir(dir)
for i in range(n):
	i += 2
	cmd = "./rod_generate "+str(i)+" r"
	os.system(cmd)
	print(cmd)
	cmd = "mv rod_"+str(i)+".xml "+dir+"rod_"+str(i)+".xml"
	os.system(cmd)
	print(cmd)
	cmd = "mv rod_"+str(i)+".cfg "+dir+"rod_"+str(i)+".cfg"
	os.system(cmd)
	print(cmd)

dir="unreachable/"
mkdir(dir)
for i in range(n):
	i += 2
	cmd = "./rod_generate "+str(i)+" u"
	os.system(cmd)
	print(cmd)
	cmd = "mv rod_"+str(i)+".xml "+dir+"rod_"+str(i)+".xml"
	os.system(cmd)
	print(cmd)
	cmd = "mv rod_"+str(i)+".cfg "+dir+"rod_"+str(i)+".cfg"
	os.system(cmd)
	print(cmd)
