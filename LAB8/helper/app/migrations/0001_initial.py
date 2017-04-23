# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='TableEntry',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('sack', models.IntegerField()),
                ('window_size', models.IntegerField()),
                ('cong_window_schemes', models.CharField(max_length=10)),
                ('link_delay', models.IntegerField()),
                ('link_drop_percent', models.FloatField()),
                ('file_size', models.IntegerField()),
                ('speed', models.FloatField()),
                ('time', models.FloatField()),
            ],
        ),
    ]
