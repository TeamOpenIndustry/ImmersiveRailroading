
import math
import os
import re
import subprocess
from PIL import Image

imgdir = "src/main/resources/assets/immersiverailroading/wiki/images"

for root, dirs, files in os.walk("ImmersiveRailroading.wiki"):
    if ".git" in root:
        continue
    for file in files:
        fname = os.path.join(root, file)
        with open(fname, 'r') as inf:
            if file == "_Sidebar.md":
                for line in inf:
                    line = line.replace("#", "").strip()
                    if line[0] == "[":
                        link = line.replace("[", "").replace("(", "").replace(")", "").split("]")[1]
                        link = link.replace("https://github.com/cam72cam/ImmersiveRailroading/wiki/", "")
                        link = link.replace("https://github.com/cam72cam/ImmersiveRailroading/wiki", "home")
                        link = link.lower()
                        print("pageEntries.add(\"%s\");" % link)
                    else:
                        print("skipLine();")
                        #print("pageEntries.add(\"%s\");" % line)
                        print("addSectionHeader(\"%s\");" % line)

                continue

            name = fname.replace("ImmersiveRailroading.wiki/", "").replace(".md", "").lower()
            ofname = "src/main/resources/assets/immersiverailroading/wiki/en_us/%s.txt" % name
            with open(ofname, 'w') as of:
                cnt = 1
                for line in inf.read().split('\n'):
                    if "![" in line:
                        rgx = r"!\[([^!\[\]]*)\]\(([^\(\)]*)\)"
                        for i, match in enumerate(re.finditer(rgx, line)):
                            txt = match.group(0)
                            label = match.group(1)
                            image = match.group(2)
                            imgname = image.split('/')[-1].lower()
                            subprocess.call(["wget", image, "-O", imgdir + "/" + imgname])

                            iw, ih = Image.open(imgdir + "/" + imgname).size
                            scale = 275.0 / iw

                            line = line.replace(txt, "[image{200, %s, %s, immersiverailroading:wiki/images/%s}]" % (int(cnt * 13), scale, imgname))

                            cnt += ((ih * scale) / 13) -1
                    if "[" in line:
                        rgx = r"\[([^!\[\]]*)\]\(([^\(\)]*)\)"
                        for i, match in enumerate(re.finditer(rgx, line)):
                            txt = match.group(0)
                            label = match.group(1)
                            link = match.group(2)
                            if "https://github.com/cam72cam/ImmersiveRailroading/wiki/" in link:
                                link = link.replace("https://github.com/cam72cam/ImmersiveRailroading/wiki/", "immersiverailroading:").lower()
                            else:
                                link += "#www"
                            line = line.replace(txt, "[link{%s}]%s[link{}]" % (link, label))

                    line = line.replace("***", "")
                    linelen = len(line)

                    if line.strip().startswith("###"):
                        line = "[prefix{n}]" + line.replace("###", "") + "[prefix{}]"
                        cnt += 0
                    if line.strip().startswith("##"):
                        line = "[prefix{l}]" + line.replace("##", "") + "[prefix{}]"
                        cnt += 1
                        
                    of.write(line + "\n")
                    cnt+=math.ceil((linelen)/58.0)
