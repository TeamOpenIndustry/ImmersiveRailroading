for mtl in $(find src/main/resources/assets/immersiverailroading/models/rolling_stock/ | grep "mtl$"); do
	echo $mtl
	dir=$(dirname $mtl)
	data=$(cat $mtl | grep map_Kd | sed -e 's/map_Kd // ')
	for fpath in $(find $dir | grep png); do
		fname=$(basename $fpath)
		if [[ $data != *"$fname"* ]]; then
			echo "$fname UNUSED"
			rm -rf $fpath
		fi
	done
done
