package main

func m1() {
	v := 1
	{
		v := 2
		v++;
	}
	
	v--;
}

func m2() {
	v := 1
	{
		v++;
	}

	v++;
}

func m3() {
	v := 1
	{
		v:= 2 
		{
			v++;
		}
		v++;
	}
	v--;
}



func m4() {
	v := 5
	{
		{
			v := 2
			v++;
		}

		v++;

		{
			v := 3
			v++;
		}
	}
	v--;
}

func m5() {
	const n = 0 // cannot be assigned
	{
		n := 1
		{
			n = 5
		}
		
		n++;
	}
}

