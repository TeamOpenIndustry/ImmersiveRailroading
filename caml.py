import sys
import json

SHIFT = "    "

def writeDict(key, value, indent):
    if key == "elements:": # IR Hack
        key = "element:"
    print("%s%s" % (indent, key))
    write(value, indent + SHIFT)

def writeList(key, value, indent):
    if key == "elements:": # IR Hack
        key = "element:"
    for v in value:
        if isinstance(v, dict):
            writeDict(key, v, indent)
        elif isinstance(v, list):
            #writeList(key, v, indent)
            raise Exception("Unsupported")
        else:
            print("%s%s %s" % (indent, key, v))

def write(block, indent):
    for key in block.keys():
        value = block[key]
        if isinstance(value, dict):
            writeDict(key + " =", value, indent)
        elif isinstance(value, list):
            writeList(key + ":", value, indent)
        elif key == "comment":
            print("%s# %s" % (indent, value))
        else:
            if isinstance(value, str):
                value = '"%s"' % value
            print("%s%s = %s" % (indent, key, value))

with open(sys.argv[1], 'r') as f:
    root = json.loads(f.read())
    write(root, "")
