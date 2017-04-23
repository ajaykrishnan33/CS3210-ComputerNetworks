from django.db import models

# Create your models here.

class TableEntry(models.Model):
	sack = models.IntegerField()
	window_size = models.IntegerField()
	cong_window_schemes = models.CharField(max_length=10)
	link_delay = models.IntegerField()
	link_drop_percent = models.FloatField()
	link_drop_str = models.CharField(max_length=10, null=True, blank=True)
	file_size = models.IntegerField()
	speed = models.FloatField() # KB/s
	time = models.FloatField() # s
